package utilitarios

import br.com.sankhya.ws.ServiceContext
import okhttp3.*
import java.util.concurrent.TimeUnit

class DiversosKT
    /**
     * Retorna o jsession e cookie da sessão corrente
     * @author Luis Ricardo Alves Santos
     * @return Pair<String, String>
     */
    @JvmName("getLoginInfo1")
    fun getLoginInfo(job: Boolean = false): Pair<String, String> {

        val cookie = if (!job) ServiceContext.getCurrent().httpRequest?.cookies?.find { cookie ->
            cookie.name == "JSESSIONID"
        } else null

        val session = ServiceContext.getCurrent().httpSessionId

        return Pair(session, "${cookie?.value}")
    }

    /*
    * * Métodos para Webservice
    * ========================================================================================
    * * Métodos para Webservice
    * ========================================================================================
    */
    val baseurl: String = ServiceContext.getCurrent().httpRequest.localAddr
    val porta = "${ServiceContext.getCurrent().httpRequest.localPort}"
    val protocol = ServiceContext.getCurrent().httpRequest.protocol.split("/")[0].toLowerCase()
    val localHost = "$protocol://$baseurl:$porta"
    val regexContainsProtocol = """"(^http://)|(^https://)"gm""".toRegex()

    /**
     * Método para realizar requisição POST HTTP/HTTPS
     * @author Luis Ricardo Alves Santos
     * @param  url: String: URL de destino para a requisição
     * @param reqBody: String: Corpo da requisição
     * @param headersParams:  Map<String, String> - Default - emptyMap(): Cabeçalhos adicionais
     * @param queryParams: Map<String, String> - Default - emptyMap(): Parâmetros de query adicionais
     * @param contentType: String - Default - "application/json; charset=utf-8": Content type do corpo da requisição(MIME)
     * @param interno: Boolean - Default - false: Valida se é um requisição interna(Sankhya) ou externa
     * @return [String]
     */
    fun post(
        url: String,
        reqBody: String,
        headersParams: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
        contentType: String = "application/json; charset=utf-8",
        interno: Boolean = false
    ): Triple<String, Headers, List<String>> {

        // Tratamento de paramentros query
        val query = queryParams.toMutableMap()
        val headers = headersParams.toMutableMap()
        var reqUrl = url

        if (interno || !url.matches(regexContainsProtocol)) {
            val loginInfo = getLoginInfo()
            if (url[0] != '/' && !url.contains("http")) reqUrl = "$localHost/$url"
            if (url[0] == '/' && !url.contains("http")) reqUrl = "$localHost$url"
            query += mapOf("jsessionid" to loginInfo.first, "mgeSession" to loginInfo.first)
//        headers["cookie"] = "JSESSIONID=${loginInfo.second}"
        }
        val httpBuilder: HttpUrl.Builder =
            HttpUrl.parse(reqUrl)?.newBuilder() ?: throw IllegalStateException("URL invalida")
        query.forEach { (name, value) ->
            httpBuilder.addQueryParameter(name, value)
        }
        val urlWithQueryParams = httpBuilder.build()

        // Instância o client
        val client = OkHttpClient().newBuilder().connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS).build()

        // Define o contentType
        val mediaTypeParse = MediaType.parse(contentType)

        // Constrói o corpo da requisição
        val body = RequestBody.create(mediaTypeParse, reqBody)

        val requestBuild = Request.Builder().url(urlWithQueryParams).post(body)
        headers.forEach { (name, value) ->
            requestBuild.addHeader(name, value)
        }
        val request = requestBuild.build()
        client.newCall(request).execute().use { response ->
            assert(response.body() != null)
            return Triple(response.body()!!.string(), response.headers(), response.headers().values("Set-Cookie"))
        }
    }
