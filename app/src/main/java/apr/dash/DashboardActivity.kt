package apr.dash

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import apr.dash.DTO.ToDo
import com.example.vicky.todolist.R
//import apr.dash.R
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.dialog_dashboard.*
import com.allyants.notifyme.NotifyMe

public class DashboardActivity : AppCompatActivity() {

    lateinit var dbHandler: DBHandler

    lateinit var notificationManager : NotificationManager
    lateinit var notificationChannel : NotificationChannel
    lateinit var builder : Notification.Builder
    private val channelId = "apr.dash"
    private val description = "Task Due!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setSupportActionBar(dashboard_toolbar)
        title = "Current Sprints 🏃‍"
        dbHandler = DBHandler(this)
        rv_dashboard.layoutManager = LinearLayoutManager(this)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        fab_dashboard.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Add Sprint Name")
            val view = layoutInflater.inflate(R.layout.dialog_dashboard, null)
            val toDoName = view.findViewById<EditText>(R.id.ev_sprint)
            dialog.setView(view)
            dialog.setPositiveButton("Add") { _: DialogInterface, _: Int ->
                if (toDoName.text.isNotEmpty()) {
                    val toDo = ToDo()
                    toDo.name = toDoName.text.toString()
                    dbHandler.addToDo(toDo)
                    refreshList()

                    val intent = Intent(this, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        notificationChannel = NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_DEFAULT)
                        notificationChannel.enableLights(true)
                        notificationChannel.lightColor = Color.GRAY
                        notificationChannel.enableVibration(false)
                        notificationManager.createNotificationChannel(notificationChannel)

                        builder = Notification.Builder(this, channelId)
                            .setContentTitle("Dash")
                            .setContentText(toDo.name + " created!")
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_run))
                            .setContentIntent(pendingIntent)
                    } else {
                        builder = Notification.Builder(this)
                            .setContentTitle("Dash")
                            .setContentText(toDo.name + " created!")
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_run))
                            .setContentIntent(pendingIntent)
                    }
                    notificationManager.notify(1234, builder.build())

                }

                dialog.setNegativeButton("Cancel") { _: DialogInterface, _: Int ->

                }            }
            dialog.show()
        }

    }

    fun updateToDo(toDo: ToDo){
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Edit Sprint")
        val view = layoutInflater.inflate(R.layout.dialog_dashboard, null)
        val toDoName = view.findViewById<EditText>(R.id.ev_sprint)
        toDoName.setText(toDo.name)
        dialog.setView(view)
        dialog.setPositiveButton("Edit") { _: DialogInterface, _: Int ->
            if (toDoName.text.isNotEmpty()) {
                toDo.name = toDoName.text.toString()
                dbHandler.updateToDo(toDo)
                refreshList()
            }
        }
        dialog.setNegativeButton("Cancel") { _: DialogInterface, _: Int ->

        }
        dialog.show()
    }

    override fun onResume() {
        refreshList()
        super.onResume()
    }

    private fun refreshList(){
        rv_dashboard.adapter = DashboardAdapter(this, dbHandler.getToDos())
    }


    class DashboardAdapter(val activity: DashboardActivity, val list: MutableList<ToDo>) :
        RecyclerView.Adapter<DashboardAdapter.ViewHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(activity).inflate(
                    R.layout.rv_child_dashboard,
                    p0,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: ViewHolder, p1: Int) {
            holder.toDoName.text = list[p1].name

            holder.toDoName.setOnClickListener {
                val intent = Intent(activity, ItemActivity::class.java)
                intent.putExtra(INTENT_TODO_ID,list[p1].id)
                intent.putExtra(INTENT_TODO_NAME,list[p1].name)
                activity.startActivity(intent)
            }

            holder.menu.setOnClickListener {
                val popup = PopupMenu(activity,holder.menu)
                popup.inflate(R.menu.dashboard_child)
                popup.setOnMenuItemClickListener {

                    when(it.itemId){
                        R.id.menu_edit ->{
                            activity.updateToDo(list[p1])
                        }
                        R.id.menu_delete ->{
                            val dialog = AlertDialog.Builder(activity)
                            dialog.setTitle("Delete this Sprint")
                            dialog.setMessage("Are you sure you want to delete this sprint?")
                            dialog.setPositiveButton("Delete") { _: DialogInterface, _: Int ->
                                activity.dbHandler.deleteToDo(list[p1].id)
                                activity.refreshList()
                            }
                            dialog.setNegativeButton("Cancel") { _: DialogInterface, _: Int ->

                            }
                            dialog.show()
                        }
                        R.id.menu_mark_as_completed ->{
                            activity.dbHandler.updateToDoItemCompletedStatus(list[p1].id,true)
                        }
                        R.id.menu_reset ->{
                            activity.dbHandler.updateToDoItemCompletedStatus(list[p1].id,false)
                        }
                    }

                    true
                }
                popup.show()
            }
        }

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val toDoName: TextView = v.findViewById(R.id.tv_todo_name)
            val menu : ImageView = v.findViewById(R.id.iv_menu)
        }
    }
}
