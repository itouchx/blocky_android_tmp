package ablockly

import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

inline fun<T> repeat(times:Int, initial:T, block:(T)->T):T{
    var t = initial
    (0 until times).forEach { t = block(t) }
    return t
}

fun buildNode(string: String):XMLNode?{
    return try {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(string))).documentElement.node()
    }catch (e:Exception){
        null
    }
}

class XMLNode{
    var name = ""
    var value = ""
    var parent:XMLNode? = null
    val attrs = hashMapOf<String, String>()
    val children = mutableListOf<XMLNode>()
    override fun toString() = name + if (children.size > 0) " child: " + children.size else " value: " + value
    val root:XMLNode?
        get(){
            var p = parent
            while (p?.parent != null) p = p.parent
            return p
        }
    val brother:XMLNode?
        get() = parent?.let {
            val idx = it.children.indexOf(this)
            if (idx > -1 && it.children.size > idx + 1) it.children[idx + 1] else null
        }
    fun forEach(block:(XMLNode)->Unit){
        block(this)
        children.forEach { it.forEach(block) }
    }

    operator fun get(path:String):XMLNode?{
        val subs = path.split(".")
        if (subs.isEmpty()) return null
        val comps = subs.map{
            val strs = it.split("[")
            if (strs.size>1){
                val subsubs = strs[1].trim(']').split("=")
                Pair(strs[0], if (subsubs.size>1) Pair(subsubs[0], subsubs[1]) else null)
            }else{
                Pair(strs[0], null)
            }
        }
        var depth = 0
        var hit = true
        var node = this
        while (true){
            if (hit){
                if (node.match(comps[depth])){
                    if (depth >= comps.size - 1){
                        return node
                    } else {
                        node.children.firstOrNull()?.let{node = it; depth += 1} ?: {hit = false; depth = maxOf(depth - 1, 0)}()
                    }
                }else{
                    hit = false
                }
            }else{
                if (depth == 0 || node == this) return null
                val parent = node.parent ?: return null
                node.brother?.let({node = it; hit = true}) ?: {node = parent; depth -= 1}()
            }
        }
    }
    private inline fun XMLNode.match(pair:Pair<String, Pair<String, String>?>) = if (name != pair.first) false else pair.second?.let { attrs[it.first] == it.second } ?: true
}

private fun Node.node():XMLNode?{
    if (nodeType == Node.ELEMENT_NODE){
        val node = XMLNode()
        node.name = nodeName
        val attrs = attributes
        (0 until attrs.length).forEach {
            val item = attrs.item(it)
            node.attrs[item.nodeName] = item.nodeValue
        }
        val ch = childNodes
        (0 until ch.length).forEach {
            ch.item(it).node()?.let {it.parent = node; node.children.add(it)}
        }
        if (childNodes.length == 1){
            val ch = childNodes.item(0)
            if (ch.nodeType == Node.TEXT_NODE) node.value = ch.nodeValue
        }
        return node
    }
    return null
}