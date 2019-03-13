package com.example.mukeshrana.xmppchat

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var xmppChatManger: XmppChatManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnConnect.setOnClickListener { init() }

        btnDisconect.setOnClickListener { disconnet() }
    }

    private fun init() {
        xmppChatManger = XmppChatManager.getInstance(this,
                "8d43eb85-e92f-4b13-968f-3cd4338c7b67", "TeGQVAXQ22du", "uihjcbkdbchjs")
    }

    private fun disconnet() {
        xmppChatManger?.disconnect()
    }
}
