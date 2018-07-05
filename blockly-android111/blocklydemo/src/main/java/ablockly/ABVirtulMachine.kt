package ablockly

fun virtulMachine(xml: String):ABVirtulMachine? = trees(xml)?.let { ABVirtulMachine(it.first, it.second, it.third) }

internal val stackTypes = listOf("controls_if", "controls_if_else", "controls_repeat_ext", "procedures_callnoreturn", "procedures_defnoreturn")

class ABVirtulMachine internal constructor(trunk: XMLNode, branches:List<XMLNode>, functions:List<XMLNode>):ABParser(trunk, branches, functions) {
    val performer = ABPerformer(this)
    private var paused = false
    private var running = false
    private val variables = linkedMapOf<String, Int>()
    private var trunkCurrent:XMLNode? = null
    private var branckCurrent:XMLNode? = null
    private val trunkStack = mutableListOf<StackContext>()
    private val branchStack = mutableListOf<StackContext>()

    fun start(){
        reset()
        running = true
        check()
    }
    fun stop(){
        if (running){
            reset()
            performer.stop()
        }
    }
    fun pause() = if (running && !paused) paused = true else Unit

    fun resume(){
        if (running && paused){
            paused = false
            check()
        }
    }
    internal inline fun endCurrent() = if (running && !paused) check() else Unit

    private fun reset(){
        trunkStack.clear()
        branchStack.clear()
        trunkCurrent = null
        branckCurrent = null
        paused = false
        running = false
    }
    private inline fun check() = next()?.let{performer.run(it)} ?: stop()

    private fun next():XMLNode?{
        val branch = (branchStack.firstOrNull()?.node ?: branckCurrent)?.root
        branches.forEach {
            val stack = mutableListOf<StackContext>()
            if (it != branch){
                next(it, stack)?.let {
                    branchStack.clear()
                    branchStack.addAll(stack)
                    branckCurrent = it
                    return it
                }
            }
        }
        if (branch != null){
            branckCurrent?.let {
                next(it, branchStack)?.let{
                    branckCurrent = it
                    return it
                }
                branchStack.clear()
                branckCurrent = null
                next(branch, branchStack)?.let {
                    branckCurrent = it
                    return it
                }
            }
        }
        next(trunkCurrent ?: trunk, trunkStack)?.let {
            trunkCurrent = it
            return it
        }
        return null
    }
    private fun next(node: XMLNode, stack:MutableList<StackContext>):XMLNode?{
        val type = node.attrs["type"]?: return null
        var next = node
        when(type){
             in stackTypes + "procedures_defnoreturn" ->{
                val context = stack.lastOrNull() ?: return null
                next = context.node
                when(type){
                    "controls_if", "controls_if_else" ->{
                        if (context.status == StackOperate.Push){
                            node["block.value[name=IF0].block"]?.let {
                                val path = if (performer.evaluate(it) == "true") "block.statement[name=DO0].block" else "block.statement[name=ELSE].block"
                                node[path]?.let {
                                    context.status = StackOperate.Update
                                    tryPush(it, stack)
                                    return it
                                }
                            }
                        }
                    }
                    "controls_repeat_ext" ->{
                        if (context.status == StackOperate.Push || context.status == StackOperate.Update){
                            node["block.value[name=TIMES].block"]?.let {
                                if (performer.evaluate(it).toInt() > context.int0){
                                    node["block.statement[name=DO].block"]?.let {
                                        context.status = StackOperate.Update
                                        context.int0 += 1
                                        tryPush(it, stack)
                                        return it
                                    }
                                }
                            }
                        }
                    }
                    "control_repeat_until" ->{

                    }
                    "control_repeat_always" ->{

                    }
                    "procedures_callnoreturn" ->{
                        if (context.status == StackOperate.Push){
                            node["field[name=NAME]"]?.let {name->
                                functions.first { it["field[name=NAME]"]?.value ==  name.value}?.let { return it }
                            }
                        }
                    }
                    "procedures_defnoreturn" ->{
                        node["block.statement[name=STACK].block"]?.let {
                            context.status = StackOperate.Update
                            tryPush(it, stack)
                            return it
                        }
                    }
                }
                 if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            }
            "start_tilt" -> return if (performer.evaluate(node) == "true") traverse(next, stack) else null
            "restart" -> {
                val root = (stack.firstOrNull()?.node ?: node).root ?: return null
                stack.clear()
                root["block.next.block"]?.let {
                    tryPush(it, stack)
                    return it
                } ?: return null
            }
        }
        return traverse(next, stack)
    }
    private fun traverse(node: XMLNode, stack: MutableList<StackContext>):XMLNode?{
        node["block.next.block"]?.let {
            tryPush(it, stack)
            return it
        }
        val last = stack.lastOrNull() ?: return null
        return when(last.node.attrs["type"]){
            null -> null
            "controls_repeat_ext" -> last.node
            else -> {
                last.status = StackOperate.Pop
                next(last.node, stack)
            }
        }
    }
    private enum class StackOperate{
        Push, Update, Pop
    }
    private class StackContext(internal val node:XMLNode){
        var status = StackOperate.Push
        var int0 = 0
        val type = node.attrs["type"] ?: ""
    }
    private fun tryPush(node:XMLNode, stack: MutableList<StackContext>){
        node.attrs["type"]?.let {
            if (it in stackTypes) stack.add(StackContext(node))
        }
    }
}