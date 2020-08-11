package app

import io.javalin.Javalin
import io.javalin.websocket.WsContext
import java.util.concurrent.ConcurrentHashMap

data class Collaboration(var doc: String = "", val clients: MutableSet<WsContext> = ConcurrentHashMap.newKeySet())

fun main() {

    val collaborations = ConcurrentHashMap<String, Collaboration>()

    Javalin.create {
        it.addStaticFiles("/public")
    }.apply {
        ws("/docs/:doc-id") { ws ->
            ws.onConnect { ctx ->
                if (collaborations[ctx.docId] == null) {
                    collaborations[ctx.docId] = Collaboration()
                }
                collaborations[ctx.docId]!!.clients.add(ctx)
                ctx.send(collaborations[ctx.docId]!!.doc)
            }
            ws.onMessage { ctx ->
                collaborations[ctx.docId]!!.doc = ctx.message()
                collaborations[ctx.docId]!!.clients.filter { it.session.isOpen }.forEach {
                    it.send(collaborations[ctx.docId]!!.doc)
                }
            }
            ws.onClose { ctx ->
                collaborations[ctx.docId]!!.clients.remove(ctx)
            }
        }
    }.start(8000)

}

val WsContext.docId: String get() = this.pathParam("doc-id")
