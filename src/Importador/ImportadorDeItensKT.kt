package Importador

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava
import br.com.sankhya.extensions.actionbutton.ContextoAcao
import br.com.sankhya.jape.core.JapeSession
import br.com.sankhya.jape.core.JapeSession.SessionHandle
import br.com.sankhya.jape.vo.DynamicVO
import br.com.sankhya.jape.wrapper.JapeFactory
import br.com.sankhya.modelcore.MGEModelException
import br.com.sankhya.ws.ServiceContext
import com.sankhya.util.TimeUtils
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.math.BigDecimal
import java.sql.Timestamp
import java.text.*
import java.util.*


class ImportadorDeItensKT : AcaoRotinaJava {
    @Throws(MGEModelException::class, IOException::class)
    override fun doAction(contextoAcao: ContextoAcao) {

        var ultilinha:Linha? = null

        val linhaPai = contextoAcao.linhas[0]
            val nunota = linhaPai.getCampo("NUNOTA") as BigDecimal?

            val arquivo = retornaVO("Anexo", "CODATA = ${nunota}") ?: throw MGEModelException("Arquivo não encontrado")
            val data = arquivo.asBlob("CONTEUDO")
            val ctx = ServiceContext.getCurrent()
            val file = File(ctx.tempFolder, "ITEMPEDIDO" + System.currentTimeMillis())

            var count = 0
            FileUtils.writeByteArrayToFile(file, data)
            try {
                BufferedReader(FileReader(file)).use { br ->
                    var line = br.readLine();
                    line = br.readLine()

                    while (line != null) {
                        if (count == 0) {
                            count++
                            continue
                        }
                        count++

                        val json = trataLinha(line)
                        ultilinha = json
                        val novaLinha = contextoAcao.novaLinha("ItemNota")

                        //Buscar descricao
                        val descrprod = retornaVO("Produto", "DESCRPROD = '${json.descricao}'")
                            ?: throw MGEModelException("Produto não encontrado")
                        val codprod = descrprod.asBigDecimal("CODPROD")

                        novaLinha.setCampo("NUNOTA", linhaPai.getCampo("NUNOTA"))
                        novaLinha.setCampo("AD_PROJPROD", json.projeto.trim())
                        novaLinha.setCampo("DTINICIO", stringToTimeStamp(json.dtprev.trim()))
                        novaLinha.setCampo("QTDNEG", convertBrlToBigDecimal(json.quantidade.trim()))
                        novaLinha.setCampo("CODPROD", codprod)
                        novaLinha.setCampo("VLRUNIT", convertBrlToBigDecimal(json.vlrunitario.trim()))
                        novaLinha.setCampo("CODLOCALORIG", BigDecimal(json.localorigem.trim()))
                        //novaLinha.setCampo("")
                        novaLinha.save()
                        line = br.readLine()
                    }
                }
            } catch (e: Exception) {
                throw MGEModelException("$e $ultilinha")
            }


    }

    private fun trataLinha(linha: String): Linha {
        var cells = if (linha.contains(";")) linha.split(";(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex()).toTypedArray()
        else linha.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex()).toTypedArray()

        cells = cells.filter { predicate ->
            if (predicate.isEmpty())
                return@filter false
            return@filter true
        }.toTypedArray() // Remove linhas vazias
        val ret = if (cells.isNotEmpty()) Linha(cells[0], cells[1], cells[2], cells[3], cells[4], cells[5]) else
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
     * Converte um valor em BRL(com ",") para [BigDecimal]
     * @author Luis Ricardo Alves Santos
     * @param str  Texto a ser convertido
     * @return [BigDecimal]
     */
    fun convertBrlToBigDecimal(str: String): BigDecimal? {
        val inId = Locale("pt", "BR")
        val nf: DecimalFormat = NumberFormat.getInstance(inId) as DecimalFormat
        nf.isParseBigDecimal = true
        return nf.parse(str, ParsePosition(0)) as BigDecimal?
    }

    /**
     * Converte uma data dd/mm/yyyy ou dd-mm-yyyy em timestamp
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

    data class Linha(
//        @JsonProperty("PROJETO")
        val projeto: String,
//        @JsonProperty("DTPREV")
        val dtprev: String,
//        @JsonProperty("QUANTIDADE")
        val quantidade: String,
//        @JsonProperty("DESCRICAO")
        val descricao: String,
//        @JsonProperty("VLRUNITARIO")
        val vlrunitario: String,
//        @JsonProperty("LOCALORIG")
        val localorigem: String

    )

}