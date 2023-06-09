package rascunho

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava
import br.com.sankhya.extensions.actionbutton.ContextoAcao
import br.com.sankhya.jape.core.JapeSession
import br.com.sankhya.jape.core.JapeSession.SessionHandle
import br.com.sankhya.jape.dao.JdbcWrapper
import br.com.sankhya.jape.sql.NativeSql
import br.com.sankhya.jape.vo.DynamicVO
import br.com.sankhya.jape.wrapper.JapeFactory
import br.com.sankhya.modelcore.MGEModelException
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper
import br.com.sankhya.modelcore.util.EntityFacadeFactory
import br.com.sankhya.ws.ServiceContext
import com.sankhya.util.JdbcUtils
import org.apache.commons.io.FileUtils
import utilitarios.mensagemErro
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.text.*
import java.util.*


class ImportadorMovInterna : AcaoRotinaJava {
    @Throws(MGEModelException::class, IOException::class)
    override fun doAction(contextoAcao: ContextoAcao) {
        var hnd: SessionHandle? = null

        var ultimaLinhaJson: LinhaJson? = null

        //Buscar nro da nota
        val linhaPai = contextoAcao.linhas[0]
        val nunota = linhaPai.getCampo("NUNOTA") as BigDecimal?
        val codEmpresa = linhaPai.getCampo("CODEMP") as BigDecimal?

        val arquivo = retornaVO("Anexo", "CODATA = ${nunota}") ?: throw MGEModelException("Arquivo não encontrado")
        val data = arquivo.asBlob("CONTEUDO")
        val ctx = ServiceContext.getCurrent()
        val file = File(ctx.tempFolder, "ITEMPEDIDO" + System.currentTimeMillis())

        var count = 0
        var countLog = 0

        FileUtils.writeByteArrayToFile(file, data)

        try {
            hnd = JapeSession.open()

            BufferedReader(FileReader(file)).use { br ->
                var line = br.readLine()
                line = br.readLine()

                while (line != null) {
                    if (count == 0) {
                        count++
                        continue
                    }
                    count++

                    val json = trataLinha(line)
                    ultimaLinhaJson = json
//                    Buscar codigo do produto usando a descrição
                    val descrprod = retornaVO("Produto", "DESCRPROD = '${json.descricao}'")
                    val codprod = descrprod?.asBigDecimal("CODPROD")
                    val codvol = descrprod?.asString("CODVOL")

//                    Buscar maior estoque do produto
                    val estoqueM = retornaVO("Estoque", "CODPROD = $codprod AND CODLOCAL <> 900000 AND ATIVO = 'S' AND ESTOQUE IN (SELECT MAX(E.ESTOQUE) AS ESTOQUE FROM TGFEST E WHERE E.CODPROD = $codprod AND E.CODLOCAL <> 900000 AND E.ESTOQUE > 0 AND E.ATIVO = 'S')")
                    val estoque = estoqueM?.asBigDecimal("ESTOQUE")

                    val qtdEstoque = if (estoque == null || estoque.equals("null")) {
                        BigDecimal.ZERO
                    } else {
                        estoque
                    }

//                    mensagemErro("$qtdEstoque")

                    val qtdJson = converterValorMonetario(json.quantidade.trim())

                    if (codprod != null && qtdJson < qtdEstoque) {
                        val novaLinha = contextoAcao.novaLinha("ItemNota")
                        novaLinha.setCampo("NUNOTA", linhaPai.getCampo("NUNOTA"))
                        novaLinha.setCampo("CODPROD", codprod)
                        novaLinha.setCampo("AD_PROJPROD", json.projeto.trim())
                        novaLinha.setCampo("AD_NRTAG", json.tag.trim())
                        novaLinha.setCampo("QTDNEG", converterValorMonetario(json.quantidade.trim()))
                        novaLinha.setCampo("CODVOL", codvol)
                        novaLinha.setCampo("CODLOCALORIG", BigDecimal.ZERO)
                        novaLinha.save()
                        line = br.readLine()
                    } else {
                        val novaLinhaLog = contextoAcao.novaLinha("AD_LOGMOVINTERNA")
                        novaLinhaLog.setCampo("NUNOTA", linhaPai.getCampo("NUNOTA"))
                        novaLinhaLog.setCampo("DESCRICAO", json.descricao.trim())
                        novaLinhaLog.setCampo("QUANTIDADE", converterValorMonetario(json.quantidade.trim()))
                        novaLinhaLog.setCampo("CODVOL", codvol)
                        novaLinhaLog.setCampo("DTLOG", getDhAtual())
                        novaLinhaLog.setCampo("NRTAG", json.tag.trim())
                        novaLinhaLog.setCampo("ERROR", "Produto não encontrado ou Estoque Insuficiente!")
                        novaLinhaLog.save()
                        line = br.readLine()
                        countLog++
                    }

                }

            }

        } catch (e: Exception) {
            throw MGEModelException("${e.localizedMessage} $ultimaLinhaJson")
        } finally {
            JapeSession.close(hnd)
        }

        val countLinhas = count-1
        //Finalmente configuramos uma mensagem para ser exibida após a execução da ação.
        val mensagem = StringBuffer()
        mensagem.append("Total de linhas processadas: ${countLinhas} ")
        mensagem.append(" \nQtd. de erros: ${countLog} ")
        mensagem.append(" \nErro: Produto não cadastrado ou sem Estoque")
        mensagem.append(" \nVerifique em Log Importação de Mov. Interna")

        contextoAcao.setMensagemRetorno(mensagem.toString())
    }

    private fun trataLinha(linha: String): LinhaJson {
        var cells = if (linha.contains(";")) linha.split(";(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex()).toTypedArray()
        else linha.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex()).toTypedArray()

        cells = cells.filter { predicate ->
            if (predicate.isEmpty())
                return@filter false
            return@filter true
        }.toTypedArray() // Remove linhas vazias
        val ret = if (cells.isNotEmpty()) LinhaJson(cells[0], cells[1], cells[2], cells[3]) else
            null

        if (ret == null) {
            throw Exception("Erro ao processar a linha: $linha")
        }
        return ret

    }

    @Throws(MGEModelException::class)
    fun retornaVO(instancia: String?, where: String?): DynamicVO? {
        var dynamicVo: DynamicVO? = null
        var hnd: SessionHandle? = null
        try {
            hnd = JapeSession.open()
            val instanciaDAO = JapeFactory.dao(instancia)
            dynamicVo = instanciaDAO.findOne(where)
        } catch (e: java.lang.Exception) {
            MGEModelException.throwMe(e)
        } finally {
            JapeSession.close(hnd)
        }
        return dynamicVo
    }

    /**
     * Converte um valor (String) com separador de milhares para [BigDecimal]
     * @author Aliny Sousa
     * @param str  Texto a ser convertido
     * @return [BigDecimal]
     */
    fun converterValorMonetario(valorMonetario: String): BigDecimal {
        val valorNumerico = valorMonetario.replace("\"", "").replace(".", "").replace(",", ".")
        return BigDecimal(valorNumerico)
    }


    /**
     * Converte uma data dd/mm/yyyy ou dd-mm-yyyy em timestampb
     * @author Luis Ricardo Alves Santos
     * @param strDate  Texto a ser convertido
     * @return [Timestamp]
     */
    fun stringToTimeStamp(strDate: String): Timestamp? {
        try {
            val formatter: DateFormat = SimpleDateFormat("MM/dd/yyyy")
            val date: Date = formatter.parse(strDate)
            return Timestamp(date.time)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Retorna a data atual
     */
    fun getDhAtual(): Timestamp {
        return Timestamp(System.currentTimeMillis())
    }

    /**
     * Recalcula os impostos dos itens e da nota
     * @author Aliny Sousa
     * @param nunota  Numero da nota para recalculo
     */
    fun recalcularImpostos(nunota: BigDecimal?) {
        val impostosHelper = ImpostosHelpper()
        impostosHelper.calcularImpostos(nunota)
        impostosHelper.totalizarNota(nunota)

//        val centralFinanceiro = CentralFinanceiro()
//        centralFinanceiro.inicializaNota(nunota)
//        centralFinanceiro.refazerFinanceiro()

    }

    data class LinhaJson(
        val tag: String,
        val projeto: String,
        val descricao: String,
        val quantidade: String
    )

}