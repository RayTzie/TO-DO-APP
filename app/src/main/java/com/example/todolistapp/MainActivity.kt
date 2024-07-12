package com.example.todolistapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var addButton: FloatingActionButton
    private lateinit var listContainer: LinearLayout
    private lateinit var mainContainer: LinearLayout
    private lateinit var welcomeTextContainer: RelativeLayout
    private lateinit var appDb: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addButton = findViewById(R.id.addButton)
        listContainer = findViewById(R.id.listContainer)
        welcomeTextContainer = findViewById(R.id.welcomeTextContainer)

        appDb = AppDatabase.getDatabase(this)

        addButton.setOnClickListener { addNewTask() }

        // Fetch all tasks from the database and display them
        GlobalScope.launch(Dispatchers.IO) {
            val tasks = appDb.taskDao().getAllTasks()

            // Check if there are tasks to display
            if (tasks.isNotEmpty()) {
                runOnUiThread {
                    welcomeTextContainer.visibility = View.GONE // Hide welcome message

                    // Loop through tasks and create a UI element for each task
                    for (task in tasks) {
                        displayTask(task)
                    }
                }
            }
        }
    }

    private fun displayTask(task: Task) {
        val todoItemLayout =
            layoutInflater.inflate(R.layout.todo_item_layout, listContainer, false) as LinearLayout

        val taskEditText = todoItemLayout.findViewById<EditText>(R.id.taskEditText)
        val checkBox = todoItemLayout.findViewById<CheckBox>(R.id.checkBox)

        // Set the task description in the EditText
        taskEditText.setText(task.description)

        // Enable or disable CheckBox based on EditText content
        taskEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                checkBox.isEnabled = s?.isNotBlank() ?: false

                GlobalScope.launch(Dispatchers.IO) {
                    // Update the task description in the database
                    task.description = s.toString()
                    appDb.taskDao().update(task)
                }
            }
        })

        // Set up CheckBox listener to remove the task with fade-out animation
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Delete the task from the database
                deleteTask(task)

                // Fade out and remove the view
                fadeOutAndRemove(todoItemLayout)
            }
        }

        // Add the new LinearLayout to the listContainer
        listContainer.addView(todoItemLayout)
    }

    private fun addNewTask() {
        // Hide Welcome TextViews
        welcomeTextContainer.visibility = View.GONE

        // Inflate new instance of todoItemLayout
        val todoItemLayout =
            layoutInflater.inflate(R.layout.todo_item_layout, listContainer, false) as LinearLayout

        // Set up EditText and CheckBox
        val taskEditText = todoItemLayout.findViewById<EditText>(R.id.taskEditText)
        val checkBox = todoItemLayout.findViewById<CheckBox>(R.id.checkBox)

        // Enable or disable CheckBox based on EditText content
        taskEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkBox.isEnabled = taskEditText.text.isNotBlank()

                GlobalScope.launch(Dispatchers.IO) {
                    appDb.taskDao().insert(Task(null, taskEditText.text.toString()))
                }
            }
        }

        // Add the new LinearLayout to the listContainer
        listContainer.addView(todoItemLayout)

        // Set focus on the EditText and show the keyboard
        taskEditText.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(taskEditText, InputMethodManager.SHOW_IMPLICIT)

        // Clear the EditText for the next input
        taskEditText.text.clear()

        // Set up CheckBox listener to remove the task with fade-out animation
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Delete the task from the database
                deleteTask(Task(null, taskEditText.text.toString()))

                // Fade out and remove the view
                fadeOutAndRemove(todoItemLayout)
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun deleteTask(task: Task) {
        GlobalScope.launch(Dispatchers.IO) {
            appDb.taskDao().delete(task)
//            appDb.taskDao().deleteAll()
        }
    }

    private fun fadeOutAndRemove(view: View) {
        view.animate().alpha(0f).setDuration(500).withEndAction {
            listContainer.removeView(view)
        }.start()
    }
}