package io.javalin.swagger

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityRequirement
import java.util.*

//#region Factory functions
fun route() = Route()

fun parameter(name: String, location: ParameterIn) = Parameter(name, location).also { ParameterBuilder.route?.add(it) }

fun content() = Content()

fun withMime(vararg mimeTypes: String) = ContentEntry(mimeTypes.toMutableList())
fun withMimeJson(mimeType: String = "application/json") = ContentEntry(mimeType)


fun withStatus(status: Int) = ResponseEntry(status.toString())

fun withStatus(status: String) = ResponseEntry(status)

//#endregion

//#region Route

class Route {
    private val response = Response(this)
    private val request = Request(this)
    private var description: String? = null
    private var summary: String? = null
    private var id: String? = null
    private var tag: String? = null
    private var deprecated: Boolean? = null
    private val parameters = mutableListOf<Parameter>()
    private val securityRequirements = mutableListOf<SecurityRequirement>()

    fun request() = request
    fun response() = response

    fun description(description: String) = this.apply { this.description = description }
    fun description() = description

    fun summary(summary: String) = this.apply { this.summary = summary }
    fun summary() = summary

    fun id(id: String) = this.apply { this.id = id }
    fun id() = id

    fun tag(tag: String) = this.apply { this.tag = tag }
    fun tag() = tag

    fun deprecated(deprecated: Boolean) = this.apply { this.deprecated = deprecated }
    fun deprecated() = deprecated

    fun securityRequirements(vararg securityRequirements: SecurityRequirement) = this.apply { this.securityRequirements.addAll(securityRequirements) }
    fun securityRequirements() = securityRequirements

    fun params(closure: () -> Unit): Route {
        synchronized(ParameterBuilder::class) {
            ParameterBuilder.start(this)
            closure()
            ParameterBuilder.build()
        }
        return this
    }

    internal fun add(parameter: Parameter) = this.apply { this.parameters.add(parameter) }

    fun params(): List<Parameter> = parameters

    fun build() = this
}

private object ParameterBuilder {

    internal var route: Route? = null

    fun start(route: Route) {
        this.route = route
    }

    fun build() {
        this.route = null
    }
}

class Parameter(private val name: String, private val location: ParameterIn) {
    private var description: String? = null
    private var required: Boolean? = null
    private var schema: FormatType? = null
    private var format: String? = null
    private var default: String? = null

    fun name() = name
    fun location() = location

    fun description(description: String) = this.apply { this.description = description }
    fun description() = description

    fun required(required: Boolean) = this.apply { this.required = required }
    fun required() = required

    fun schema(schema: Class<*>) = this.apply { this.schema = FormatType.getByClass(schema) }
    fun schema() = schema

    fun format(format: String) = this.apply { this.format = format }
    fun format() = format

    fun asSwagger() = io.swagger.v3.oas.models.parameters.Parameter()
            .name(this.name())
            .description(this.description())
            .`in`(this.location().toString())
            .required(this.required())
            .schema(this.schema()?.getSchema())
}

//#endregion

//#region Request

class Request(private val route: Route) {
    private var description: String? = null
    private var content: Content? = null
    private var required: Boolean = false

    fun response() = route.response()

    fun description(description: String) = this.apply { this.description = description }
    fun description() = description

    fun content(content: Content) = this.apply { this.content = content }
    fun content() = content

    fun required(required: Boolean) = this.apply { this.required = required }
    fun required() = required

    fun build() = route
}

class Content {
    private val entries = mutableListOf<ContentEntry>()

    fun entry(entry: ContentEntry) = this.also { this.entries.add(entry) }
    fun entries(): List<ContentEntry> = entries
}

class ContentEntry(private var mimeTypes: MutableList<String>) {
    constructor(vararg mimeTypes: String) : this(mimeTypes.toMutableList())

    private var schema: Class<*>? = null
    private var example: Any? = null

    fun mimes() = mimeTypes

    fun schema(schema: Class<*>) = this.also { this.schema = schema }
    fun schema() = schema as? Class<Any>

    fun example(example: Any) = this.also { this.example = example }
    fun example(): Any? = example

    fun withMime(mimeType: String) = this.also { this.mimeTypes.add(mimeType) }

    fun asMediaType(): MediaType {
        val entry = this
        return MediaType()
                .schema(entry.schema()?.parseSchema(entry.example()))
    }
}

//#endregion

//#region Response

class Response(private val route: Route) {
    private val entries = mutableListOf<ResponseEntry>()

    fun add(entry: ResponseEntry) = this.also { entries.add(entry) }
    fun entries(): List<ResponseEntry> = entries

    fun build() = route

    fun request() = route.request()
}

class ResponseEntry(private val status: String) {
    private var description: String? = null
    private var content: Content? = null
    private var headers: Array<out Header>? = null

    fun status() = status

    fun description(description: String) = this.apply { this.description = description }
    fun description() = description

    fun content(content: Content) = this.apply { this.content = content }
    fun content() = content

    fun headers(vararg headers: Header) = this.apply { this.headers = headers }
    fun headers(): MutableMap<String, io.swagger.v3.oas.models.headers.Header>? {
        var headersOfSwagger: MutableMap<String, io.swagger.v3.oas.models.headers.Header>? = null
        headers?.apply {
            headersOfSwagger = mutableMapOf()
        }?.forEach { header ->
            headersOfSwagger!![header.name] = header.asSwaggerType()
        }
        return headersOfSwagger
    }
}

class Header(val name: String) {
    private var schema: FormatType? = null
    private var description: String? = null

    fun schema(): FormatType? {
        return schema
    }

    fun <T> schema(clazz: Class<T>): Header {
        this.schema = FormatType.getByClass(clazz)
        return this
    }

    fun description(description: String) = this.apply { this.description = description }

    fun asSwaggerType(): io.swagger.v3.oas.models.headers.Header {
        return io.swagger.v3.oas.models.headers.Header()
                .description(this.description)
                .schema(this.schema()?.getSchema())
    }
}
//#endregion

enum class FormatType {
    INT32(Int::class.java, "integer", "int32"),
    INT64(Long::class.java, "integer", "int64"),
    FLOAT(Float::class.java, "number", "float"),
    DOUBLE(Double::class.java, "number", "double"),
    STRING(String::class.java, "string", null),
    BYTE(Byte::class.java, "string", "byte"),
    BOOLEAN(Boolean::class.java, "boolean", null),
    DATE(Date::class.java, "string", "date"),
    ENUM(Enum::class.java, "string", null);
    //    DATETIME(Date::class.java, "strng", "date-time"), TODO look for class in kotlin to replace Date
    //PASSWORD(String::class.java, "string", "password")

    private val clazz: Class<*>
    val type: String
    val format: String?

    constructor(clazz: Class<*>, type: String, format: String?) {
        this.clazz = clazz
        this.type = type
        this.format = format
    }

    fun getSchema(): Schema<*> {
        return Schema<Any>().type(this.type).format(this.format)
    }

    companion object {
        fun getByClass(clazz: Class<*>?): FormatType? {
            if (clazz == null) return null
            return values().find { it.clazz == clazz }
                    ?: values().find { it.clazz == clazz.superclass }
        }

    }
}