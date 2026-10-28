package pe.upeu.ecorecicla
import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.data.local.BaseLocal
fun crearBaseLocal(context: Context)=BaseLocal(EcoDatabase(AndroidSqliteDriver(EcoDatabase.Schema,context,"ecorutas.db")))
