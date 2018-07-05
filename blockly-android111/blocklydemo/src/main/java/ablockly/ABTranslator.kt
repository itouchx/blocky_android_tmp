package ablockly

import kotlin.collections.LinkedHashMap

fun translator(xml:String, rules: LinkedHashMap<String, String>):ABTranslator? = trees(xml)?.let { ABTranslator(it.first, it.second, it.third, rules) }

internal fun parseRule(str:String):LinkedHashMap<String, String>{
    val map = linkedMapOf<String, String>()
    str.split(",\n").forEach {
        val arr = it.split(":\n")
        if (arr.size > 1) map[arr[0].trimMargin()] = arr[1].trimMargin()
    }
    return map
}

private val tab = "  "

class ABTranslator internal constructor(trunk: XMLNode, branches:List<XMLNode>, functions:List<XMLNode>, private val rules:LinkedHashMap<String, String>):ABParser(trunk, branches, functions) {
    var codes = ""
        private set

    init {
        var str = ""
        if (!functions.isEmpty()){
            str += "//function declarations\\n"
            functions.forEach{str += code(it, 0)}
            str += "\n"
        }
        str += "//trunk\n" + code(trunk, 0)
        branches.forEachIndexed { idx, node -> str += "\n//branch ${idx+1}\n" + code(node, 0) }
        codes = str
    }

    private fun code(node:XMLNode, depth:Int):String{
        val type = node.attrs["type"] ?: return ""
        val rule = rules[type] ?: return ""
        var str = if (depth > 0) rule.replace("\n", "\n" + repeat(depth, ""){it + tab}) else rule
        val reg = "\\\\\\([a-zA-Z0-9\\[=\\]$]*\\)".toRegex()
        var offset = 0
        while (true){
            val range = reg.find(str, offset) ?: break
            var path = str.substring(range.range.start + 2, range.range.last)
            val split = path.split('\$')
            var def = ""
            if (split.size > 1){
                path = split[0]
                def = split[1]
            }
            if (!path.startsWith("block")) path = "block." + path
            if (!path.endsWith("block") && "field" !in path) path += ".block"
            var repl = ""
            node[path]?.let {
                when{
                    it.name == "field" -> repl = it.value
                    "statement" in path || type.startsWith("start") ->{
                        val c = code(it, depth + 1)
                        if (!c.isEmpty()) repl = "\n" + repeat(depth + 1, ""){it + tab} + c
                    }
                    else -> repl = code(it, depth)
                }
            }
            val res = if (repl.isEmpty()) def else repl
            str = str.replaceRange(range.range, res)
            offset = range.range.first + res.length
        }
        if (!type.startsWith("start")) node["block.next.block"]?.let { str += "\n" + repeat(depth, ""){it + tab} + code(it, depth) }
        return str
    }
}