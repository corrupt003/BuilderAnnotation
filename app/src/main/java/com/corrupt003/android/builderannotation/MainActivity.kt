package com.corrupt003.android.builderannotation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val user = UserBuilder()
            .name("corrupt003")
            .age(18)
            .build()

        val message = "User name: ${user.name}, age: ${user.age}"
        main_message.text = message
    }
}
