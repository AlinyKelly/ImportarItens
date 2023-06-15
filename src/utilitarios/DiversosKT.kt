package utilitarios

import br.com.sankhya.modelcore.MGEModelException
import br.com.sankhya.ws.ServiceContext
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.script.Invocable
import javax.script.ScriptEngineManager

val gson: Gson = GsonBuilder().setPrettyPrinting().create()

class DiversosKT

val functionNameRegex = "(function)+[\\s]([a-zA-Z_{1}][a-zA-Z0-9_]+)(?=\\()".toRegex()
val removeCommentsRegex = "\"(^(\\/\\*+[\\s\\S]*?\\*\\/)|(\\/\\*+.*\\*\\/)|\\/\\/.*?[\\r\\n])[\\r\\n]*\"gm".toRegex()
data class GetPropertyFromObject(val data: Any?, val type: String)

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

@Throws(java.lang.Exception::class)
fun loadResource(
    baseClass: Class<*> = Class.forName(Thread.currentThread().stackTrace[2].className),
    resourcePath: String
): String {
    return getContentFromResource(baseClass, resourcePath)
}

/*
* * Métodos para utilizar resources
* ========================================================================================
* * Métodos para utilizar resources
* ========================================================================================
*/
@Throws(java.lang.Exception::class)
fun getContentFromResource(baseClass: Class<*>, resourcePath: String): String {
    val stream = baseClass.getResourceAsStream(resourcePath)
        ?: throw IllegalArgumentException("Arquivo não nencontrado(${baseClass.name}):$resourcePath")

    return BufferedReader(
        InputStreamReader(stream, StandardCharsets.UTF_8)
    )
        .lines()
        .collect(Collectors.joining("\n"))
}

/**
 * Executa uma função javascript e retorna o valor
 * @author Luis Ricardo Alves Santos
 * @param script  Nome da propriedade
 * @param args JSON
 * @return [Any?]
 */
fun runJSFunction(script: String, vararg args: Any?): Any? {
    val manager = ScriptEngineManager()
    val engine = manager.getEngineByName("JavaScript")
    val name = functionNameRegex.find(script)?.groupValues?.get(2)
    val inputScript = script.replace(removeCommentsRegex, "")
    engine.eval(inputScript)
    val invoker = engine as Invocable
    return invoker.invokeFunction(name, *args)
}

/**
 * Retorna o valor de um json
 * @author Luis Ricardo Alves Santos
 * @param prop  Nome da propriedade
 * @param json JSON
 * @return [String]
 */
fun getPropFromJSON(prop: String, json: String): String {
    val script = loadResource(DiversosKT::class.java, "resources/getPropertyFromObject.js")
    val value = runJSFunction(script, json, prop)
    val valueObject = gson.fromJson<GetPropertyFromObject>("$value", GetPropertyFromObject::class.java)
    return "${valueObject.data}"
}

@Throws(MGEModelException::class)
fun mensagemErro(mensagem: String?) {
    try {
        throw MGEModelException(mensagem)
    } catch (e: Exception) {
        MGEModelException.throwMe(e)
    }
}