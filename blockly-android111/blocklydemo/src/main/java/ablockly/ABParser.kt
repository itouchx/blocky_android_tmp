package ablockly

fun trees(xml:String):Triple<XMLNode, List<XMLNode>, List<XMLNode>>?{
    val root = buildNode(xml)
    root?.forEach {
        if (it.name == "shadow"){
            it.name = "block"
        }else if (it.attrs["type"] == "procedures_callnoreturn"){
            val node = XMLNode()
            node.name = "field"
            node.attrs["name"] = "NAME"
            node.parent = it
            node.value = it["mutation[name]"]?.attrs?.get("name") ?: ""
            it.children.add(node)
        }
    } ?: return null
    val f = mutableListOf<XMLNode>()
    var t:XMLNode? = null
    val b = mutableListOf<XMLNode>()
    root.children.forEach {
        val id = it.attrs["type"]
        when{
            id == "start" -> t = it
            id == "procedures_defnoreturn" -> f.add(it)
            id?.startsWith("start") ?: false -> b.add(it)
        }
    }
    return t?.let { Triple(it, b, f) }
}

open class ABParser internal constructor(protected val trunk:XMLNode, protected val branches:List<XMLNode>, protected val functions:List<XMLNode>)