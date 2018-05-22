package io.javalin.swagger

import io.swagger.v3.oas.annotations.enums.ParameterIn

//#region Factory functions

fun route() = Route()

fun parameter(name: String, location: ParameterIn) = Parameter(name, location)

fun content() = Content()

fun withMime(mimeType: String) = ContentEntry(mimeType)

fun withStatus(status: Int) = ResponseEntry(status.toString())

fun withStatus(status: String) = ResponseEntry(status)

//#endregion

//#region Route

class Route {
    private val response = Response(this)
    private val request = Request(response, this)
    private var description: String? = null
    private var id: String? = null
    private val parameters = mutableListOf<Parameter>()

    fun request() = request
    fun response() = response

    fun description(description: String) = this.apply { this.description = description }
    fun description() = description

    fun id(id: String) = this.apply { this.id = id }
    fun id() = id

    fun add(parameter: Parameter) = this.apply { this.parameters.add(parameter) }
    fun params(): List<Parameter> = parameters

    fun build() = this
}

class Parameter(private val name: String, private val location: ParameterIn) {
    private var description: String? = null
    private var required: Boolean? = null
    private var schema: Class<*>? = null

    fun name() = name
    fun location() = location

    fun description(description: String) = this.apply { this.description = description }
    fun description() = description

    fun required(required: Boolean) = this.apply { this.required = required }
    fun required() = required

    fun schema(schema: Class<*>) = this.apply { this.schema = schema }
    fun schema() = schema
}

//#endregion

//#region Request

class Request(private val response: Response, private val route: Route) {
    private var description: String? = null
    private var content: Content? = null

    fun response() = response

    fun description(description: String) = this.apply { this.description = description }
    fun description() = description

    fun content(content: Content) = this.apply { this.content = content }
    fun content() = content

    fun build() = route
}

class Content {
    private val entries = mutableListOf<ContentEntry>()

    fun entry(entry: ContentEntry) = this.also { this.entries.add(entry) }
    fun entries(): List<ContentEntry> = entries
}

class ContentEntry(private val mimeType: String) {
    private var schema: Class<*>? = null
    private val examples = mutableMapOf<String, Any>()

    fun mime() = mimeType

    fun schema(schema: Class<*>) = this.also { this.schema = schema }
    fun schema() = schema

    fun example(name: String, example: Any) = this.also { examples[name] = example }
    fun examples(): Map<String, Any> = examples
}

//#endregion

//#region Response

class Response(private val route: Route) {
    private val entries = mutableListOf<ResponseEntry>()

    fun add(entry: ResponseEntry) = this.also { entries.add(entry) }
    fun entries(): List<ResponseEntry> = entries

    fun build() = route
}

class ResponseEntry(private val status: String) {
    private var description: String? = null
    private var content: Content? = null

    fun status() = status

    fun description(description: String) = this.apply { this.description = description }
    fun description() = description

    fun content(content: Content) = this.apply { this.content = content }
    fun content() = content
}

//#endregion
