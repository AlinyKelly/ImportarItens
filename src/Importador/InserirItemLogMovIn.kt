package Importador
//Funciona
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava
import br.com.sankhya.extensions.actionbutton.ContextoAcao
import br.com.sankhya.jape.core.JapeSession
import br.com.sankhya.modelcore.MGEModelException
import utilitarios.getPropFromJSON
import utilitarios.post
import java.math.BigDecimal

class InserirItemLogMovIn : AcaoRotinaJava {
    @Throws(Exception::class)
    override fun doAction(contextoAcao: ContextoAcao) {
        var hnd: JapeSession.SessionHandle? = null

        val linhasSelecionadas = contextoAcao.linhas
        var nunota: BigDecimal? = null
        var codprod: BigDecimal? = null
        var quantidadeItem: BigDecimal? = null
        var codvol: String? = ""
        var projeto: String? = ""
        var tag: String? = ""

        try {
            for (linha in linhasSelecionadas) {
                nunota = linha.getCampo("NUNOTA") as BigDecimal?
                codprod = linha.getCampo("CODPROD") as BigDecimal?
                quantidadeItem = linha.getCampo("QUANTIDADE") as BigDecimal?
                codvol = linha.getCampo("CODVOL") as String?
                projeto = linha.getCampo("PROJETO") as String?
                tag = linha.getCampo("NRTAG") as String?

                val jsonItem = """{
                                "serviceName": "CACSP.incluirNota",
                                "requestBody": {
                                    "nota": {
                                        "cabecalho": {
                                            "NUNOTA": {
                                                "${'$'}": "$nunota"
                                            }
                                        },
                                        "itens": {
                                            "INFORMARPRECO": "False",
                                            "item": [
                                                {
                                                    "NUNOTA": {
                                                        "${'$'}": "$nunota"
                                                    },
                                                    "CODPROD": {
                                                        "${'$'}": "$codprod"
                                                    },
                                                    "SEQUENCIA": {
                                                        "${'$'}": ""
                                                    },
                                                    "QTDNEG": {
                                                        "${'$'}": "$quantidadeItem"
                                                    },
                                                    "CODLOCALORIG": {
                                                        "${'$'}": "0"
                                                    },
                                                    "CODVOL": {
                                                        "${'$'}": "$codvol"
                                                    },
                                                    "AD_PROJPROD": {
                                                        "${'$'}": "$projeto"
                                                    },
                                                    "AD_NRTAG": {
                                                        "${'$'}": "$tag"
                                                    }
                                                }
                                            ]
                                        }
                                    }
                                }
                            }""".trimIndent()

                val (postbody) = post("mgecom/service.sbr?serviceName=CACSP.incluirNota&outputType=json", jsonItem)
                val status = getPropFromJSON("status", postbody)
                val statusMessage = getPropFromJSON("statusMessage", postbody)
                if (status != "1") {
                    throw MGEModelException("\nError: $statusMessage")
                }

            }

            //Finalmente configuramos uma mensagem para ser exibida após a execução da ação.
            val mensagem = StringBuffer()
            mensagem.append("Itens inseridos com sucesso.")
            contextoAcao.setMensagemRetorno(mensagem.toString())

        } catch (e: Exception) {
            contextoAcao.setMensagemRetorno(e.toString())
        }
    }
}