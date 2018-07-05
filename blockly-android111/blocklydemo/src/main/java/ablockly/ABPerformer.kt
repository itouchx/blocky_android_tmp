package ablockly

import java.util.*
import kotlin.concurrent.schedule
import android.os.Handler

interface ABPerformerDelegate{
    fun highlight(id:String)
    fun unhighlight(id:String)
    fun run(cmd:String, values:HashMap<String, String>)
    fun stop()
}

class ABPerformer(private val vm:ABVirtulMachine){
    var delegate:ABPerformerDelegate? = null
    private val variables = mutableMapOf<String, MutableMap<Int, Int>>()
    private val timeInterval = 200L
    private var current:XMLNode? = null
    private var replied = false
    private var beginTime = 0L
    private var timer:Timer?= null
    fun continuee(){
        val time = Date().time - beginTime
        if (time < timeInterval){
            timer = Timer()
            val handler = Handler()
            timer?.schedule(timeInterval - time){handler.post{ continueSoon()}}
        }else{
            continueSoon()
        }
    }
    internal inline fun update(value:Int, type:String, id:Int) = variables[type]?.let { it[id] = value } ?: {variables[type] = mutableMapOf(id to value)}()
    internal fun evaluate(node:XMLNode):String{
        if (node.name == "field") return node.value
        val type = node.attrs["type"] ?: return ""
        when(type){
            "logic_compare" ->{
                val a = node["block.value[name=A].block"] ?: return "false"
                val b = node["block.value[name=B].block"] ?: return "false"
                val op = node["block.field"] ?: return "false"
                return when(evaluate(op)){
                    "=" -> (evaluate(a) == evaluate(b)).toString()
                    ">" -> (evaluate(a).toInt() > evaluate(b).toInt()).toString()
                    ">=" -> (evaluate(a).toInt() >= evaluate(b).toInt()).toString()
                    "<" -> (evaluate(a).toInt() < evaluate(b).toInt()).toString()
                    "<=" -> (evaluate(a).toInt() <= evaluate(b).toInt()).toString()
                    else -> "false"
                }
            }
            "math_arithmetic" ->{
                val a = node["block.value[name=A].block"] ?: return "false"
                val b = node["block.value[name=B].block"] ?: return "false"
                val op = node["block.field"] ?: return "false"
                return when(evaluate(op)){
                    "+" -> (evaluate(a).toInt() + evaluate(b).toInt()).toString()
                    "-" -> (evaluate(a).toInt() - evaluate(b).toInt()).toString()
                    "*" -> (evaluate(a).toInt() * evaluate(b).toInt()).toString()
                    "/" -> (evaluate(a).toInt() / evaluate(b).toInt()).toString()
                    else -> "0"
                }
            }
            "logic_operation" ->{
                val a = node["block.value[name=A].block"] ?: return "false"
                val b = node["block.value[name=B].block"] ?: return "false"
                val op = node["block.field"] ?: return "false"
                return when(evaluate(op)){
                    "AND" -> (evaluate(a) == "true" && evaluate(b) == "true").toString()
                    "OR" -> (evaluate(a) == "true" || evaluate(b) == "true").toString()
                    else -> "false"
                }
            }
            "math_number" -> return node.children.firstOrNull()?.let { evaluate(it) } ?: "0"
            "color_picker" -> return ""
            "color_random" -> return (Math.random() * 0xffffff).toInt().toString()
            "start_tilt" ->{
                val n = node["block.field[name=DIR"] ?: return "false"
                val list = variables["phone_tilt"] ?: return "false"
                return ((list[1] ?: 0) - 1 == direction(evaluate(n))?.ordinal ?: -2).toString()
            }
            else -> return ""
        }
    }
    internal fun run(node:XMLNode){
        replied = false
        timer?.cancel()
        beginTime = Date().time
        current = node
        val id = node.attrs["id"]
        if (id == null){
            continuee()
            return
        }
        delegate?.highlight(id)
        val type = node.attrs["type"]
        println("begin $type")
        when(type){
            "control_wait" -> {
                node["block.value[name=delay].block"]?.let {
                    timer = Timer()
                    val handler = Handler()
                    timer?.schedule(maxOf(it.value.toLong(), timeInterval)){handler.post{ continueSoon()}}
                }
            }
            "control_wait_until" -> {

            }
            "turtle_move" ->{
                node["block.value[name=VALUE].block"]?.let {
                    delegate?.run("move", hashMapOf("data" to evaluate(it)))
                } ?: continuee()
            }
            "turtle_turn" ->{
                node["block.value[name=VALUE].block"]?.let {
                    delegate?.run("turn", hashMapOf("data" to evaluate(it)))
                } ?: continuee()
            }
            "turtle_color" ->{
                node["block.value[name=COLOUR].block"]?.let {
                    delegate?.run("color", hashMapOf("data" to evaluate(it)))
                } ?: continuee()
            }
            "control_repeat_times", "control_repeat_until", "control_repeat_always", "control_if", "control_if_else", "restart" -> continuee()
            else -> continuee()
        }
    }
    internal fun stop(){
        current?.attrs?.get("id")?.let { delegate?.unhighlight(it) }
        current = null
        timer?.cancel()
        delegate?.stop()
    }
    private fun continueSoon(){
        current?.attrs?.get("id")?.let { delegate?.unhighlight(it) }
        current = null
        timer?.cancel()
        vm.endCurrent()
    }
}

fun direction(string: String):Direction?{
    return when(string){
        "forward" -> Direction.Forward
        "backward" -> Direction.Backward
        "left" -> Direction.Left
        "right" -> Direction.Right
        else -> null
    }
}

enum class Direction(){
    Forward, Backward, Left, Right
}