package ablockly

import android.util.Range
import java.util.*


class ABColorLexer(codes:String, keywords:List<String>){
    val colors = mutableMapOf<IntRange, Kind>()

    companion object{
        val KeywordsSwift = listOf("func", "for", "while", "in", "if", "else", "true", "false", "var")
    }
    init {
        var offset = 0
        var str = codes
        var last = -1
        val com = regex(Kind.Comment)
        val id = regex(Kind.Identifier)
        val op = regex(Kind.Operator)
        val num = regex(Kind.Number)
        if (com != null && id != null && op != null && num != null){
            while (last + 1 < str.length) {
                offset += last + 1
                str = str.substring(last + 1)
                last = 0
                var res = com.find(str)
                if (res != null){
                    colors[res.range.move(offset)] = Kind.Comment
                    last = res.range.last
                    continue
                }
                res = id.find(str)
                if (res != null){
                    val sub = codes.substring(res.range)
                    colors[res.range.move(offset)] = if (sub in keywords){
                        Kind.Keyword
                    }else if (res.range.last + 1 < codes.length && codes[res.range.last + 1] == '('){
                        Kind.Function
                    }else{
                        Kind.Identifier
                    }
                    last = res.range.last
                    continue
                }
                res = op.find(str)
                if (res != null){
                    colors[res.range.move(offset)] = Kind.Operator
                    last = res.range.last
                    continue
                }
                res = num.find(str)
                if (res != null){
                    colors[res.range.move(offset)] = Kind.Number
                    last = res.range.last
                    continue
                }
            }
        }
    }

    enum class Kind{
        Comment, Operator, Number, Keyword, Function, Identifier
    }
    private fun regex(kind:Kind):Regex?{
        return when(kind){
            Kind.Comment -> "^//.*"
            Kind.Number -> "^0x([1-9a-fA-F][1-9a-fA-F]*|[0-9a-fA-F])|^([1-9][0-9]+|[0-9])(.[0-9]+)?"
            Kind.Identifier -> "^[a-zA-Z][_a-zA-Z0-9]*"
            Kind.Operator -> "^[-+*/.,:<>=()\\[\\]{}&|]"
            else -> null
        }?.toRegex()
    }
    fun IntRange.move(step:Int):IntRange{
        return IntRange(start + step, endInclusive + step)
    }
}