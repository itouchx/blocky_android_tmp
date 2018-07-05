package com.google.blockly.android.demo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import java.io.InputStreamReader
import ablockly.*

class TestActivity : AppCompatActivity(), ABPerformerDelegate {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        run()
        translate()
        color()
    }
    private fun run(){
        var str:String? = null
        try {
            val stream = this.assets.open("src.xml")
            str = InputStreamReader(stream).readLines().joinToString("\n")
        }catch (e:Exception){
            println(e)
            return
        }
        vm = virtulMachine(str)
        vm?.let {
            it.performer.delegate = this
            it.start()
        }
    }

    private fun translate(){
        var rules: LinkedHashMap<String, String>? = null
        var str:String? = null
        try {
            var stream = this.assets.open("swift_rules")
            str = InputStreamReader(stream).readLines().joinToString("\n")
            rules = parseRule(str)
            stream = this.assets.open("src.xml")
            str = InputStreamReader(stream).readLines().joinToString("\n")
        }catch (e:Exception){
            println(e)
            return
        }
        translator(str, rules)?.let { println(it.codes) }
    }

    private fun color(){
        ABColorLexer(codes, ABColorLexer.KeywordsSwift).colors.forEach {
            println(it.key + " " + it.value)
        }
    }

    override fun highlight(id:String){
        println("high:" + id)
        vm?.performer?.continuee()
    }
    override fun unhighlight(id:String) = println("unhigh:" + id)
    override fun run(cmd:String, values: HashMap<String, String>){
        println("cmd:" + cmd)
    }
    override fun stop() = println("end")
}

private var vm:ABVirtulMachine? = null



val codes = """
for _ in 0..<10
{
    move(dir:.forward, len:50)
    turn(dir:.right, angle:90)
    color(0xff0000)
}
//branch
if 5 == 5
{
    move(dir:.forward, len:12+18)
}else
{
    turn(dir:.right, angle:90)
}
""".trim()