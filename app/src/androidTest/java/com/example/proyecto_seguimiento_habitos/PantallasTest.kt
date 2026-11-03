package com.example.proyecto_seguimiento_habitos

import com.example.proyecto_seguimiento_habitos.modelo.*
import com.example.proyecto_seguimiento_habitos.viewmodels.*
import com.example.proyecto_seguimiento_habitos.pantallas.*

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.proyecto_seguimiento_habitos.ui.theme.Proyecto_Seguimiento_HabitosTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

// Pruebas de las pantallas en el emulador.
// Los hábitos se ponen a mano en el ViewModel, así no se inicia sesión ni se toca Firestore.
@RunWith(AndroidJUnit4::class)
class PantallasTest {

    @get:Rule
    val prueba = createComposeRule()

    private val hoy = LocalDate.now().toString()

    // ViewModel con 3 hábitos de ejemplo: uno pendiente, uno hecho hoy y uno archivado
    private fun habitosDeEjemplo(): HabitosViewModel {
        val habitosViewModel = HabitosViewModel()
        habitosViewModel.habitos = listOf(
            Habito(id = "1", nombre = "Leer 20 páginas", categoria = "Estudio"),
            Habito(id = "2", nombre = "Meditar", categoria = "Salud", completados = listOf(hoy)),
            Habito(id = "3", nombre = "Correr", categoria = "Ejercicio", archivado = true)
        )
        habitosViewModel.cargando = false
        habitosViewModel.puntos = 250   // nivel 3
        return habitosViewModel
    }

    // Abre la app en la ruta indicada, con las mismas pantallas que AppNavigation
    private fun abrir(rutaInicial: String) {
        val authViewModel = AuthViewModel(ApplicationProvider.getApplicationContext())
        authViewModel.nombre = "Ana Pérez"
        authViewModel.correo = "ana@gmail.com"
        val habitosViewModel = habitosDeEjemplo()

        prueba.setContent {
            Proyecto_Seguimiento_HabitosTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = rutaInicial) {
                    composable("login") { LoginScreen(navController, authViewModel) }
                    composable("register") { RegisterScreen(navController, authViewModel) }
                    composable("inicio") { DashboardScreen(navController, authViewModel, habitosViewModel) }
                    composable("perfil") { PerfilScreen(navController, authViewModel, habitosViewModel) }
                    composable("crear") { CrearHabitoScreen(navController, habitosViewModel, null) }
                    composable("editar/{id}") { ruta ->
                        val habito = habitosViewModel.buscar(ruta.arguments?.getString("id"))
                        if (habito != null) CrearHabitoScreen(navController, habitosViewModel, habito)
                    }
                    composable("calendario") { CalendarioScreen(navController, habitosViewModel, null) }
                    composable("calendario/{id}") { ruta ->
                        CalendarioScreen(navController, habitosViewModel, ruta.arguments?.getString("id"))
                    }
                    composable("logros") { LogrosScreen() }
                }
            }
        }
    }

    // ¿Hay al menos un elemento con ese texto?
    private fun existe(texto: String): Boolean {
        return prueba.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
    }

    ////////////////// Login y registro

    @Test
    fun login_camposVaciosMuestranErrores() {
        abrir("login")
        prueba.onNodeWithText("Bienvenido de vuelta").assertIsDisplayed()
        prueba.onNodeWithText("Iniciar sesión").performClick()
        prueba.onNodeWithText("Ingresa tu correo").assertIsDisplayed()
        prueba.onNodeWithText("Ingresa tu contraseña").assertIsDisplayed()
    }

    @Test
    fun login_botonCrearCuentaAbreRegistro() {
        abrir("login")
        prueba.onNodeWithText("Crear una cuenta").performScrollTo().performClick()
        prueba.onNodeWithText("Comienza tu viaje").assertIsDisplayed()
    }

    @Test
    fun registro_camposVaciosMuestranErrores() {
        abrir("register")
        prueba.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        assertTrue(existe("Ingresa tu nombre"))
        assertTrue(existe("Ingresa tu correo"))
        assertTrue(existe("Ingresa una contraseña"))
        assertTrue(existe("Debes aceptar los términos para continuar"))
    }

    ////////////////// Inicio (dashboard)

    @Test
    fun inicio_muestraSaludoYHabitosDeHoy() {
        abrir("inicio")
        prueba.onNodeWithText("Hola, Ana").assertIsDisplayed()
        prueba.onNodeWithText("1 de 2 hábitos").assertIsDisplayed()
        prueba.onNodeWithText("Pendientes hoy").assertIsDisplayed()
        prueba.onNodeWithText("Leer 20 páginas").assertIsDisplayed()
        prueba.onNodeWithText("Completados hoy").assertIsDisplayed()
        prueba.onNodeWithText("Meditar").assertIsDisplayed()
        // Los archivados no aparecen en el inicio
        assertTrue(!existe("Correr"))
    }

    @Test
    fun inicio_tocarTarjetaAbreOpcionesYDialogoDeBorrar() {
        abrir("inicio")
        prueba.onNodeWithText("Leer 20 páginas").performClick()
        prueba.onNodeWithText("Ver progreso").assertIsDisplayed()
        prueba.onNodeWithText("Editar hábito").assertIsDisplayed()
        prueba.onNodeWithText("Archivar hábito").assertIsDisplayed()
        prueba.onNodeWithText("Eliminar hábito").performClick()
        prueba.onNodeWithText("¿Eliminar \"Leer 20 páginas\"?").assertIsDisplayed()
        prueba.onNodeWithText("Cancelar").performClick()
        assertTrue(!existe("¿Eliminar \"Leer 20 páginas\"?"))
    }

    @Test
    fun inicio_editarAbreElFormularioConLosDatos() {
        abrir("inicio")
        prueba.onNodeWithText("Leer 20 páginas").performClick()
        prueba.onNodeWithText("Editar hábito").performClick()
        prueba.onNodeWithText("Guardar cambios").performScrollTo().assertIsDisplayed()
        assertTrue(existe("Leer 20 páginas"))
    }

    @Test
    fun inicio_verProgresoAbreElCalendarioDelHabito() {
        abrir("inicio")
        prueba.onNodeWithText("Leer 20 páginas").performClick()
        prueba.onNodeWithText("Ver progreso").performClick()
        prueba.onNodeWithText("Progreso").assertIsDisplayed()
        prueba.onNodeWithText("Resumen del hábito").performScrollTo().assertIsDisplayed()
    }

    ////////////////// Crear hábito

    @Test
    fun crear_formularioVacioMuestraErrores() {
        abrir("inicio")
        prueba.onNodeWithText("Añadir").performClick()
        prueba.onNodeWithText("Nuevo hábito").assertIsDisplayed()
        prueba.onNodeWithText("Guardar hábito").performScrollTo().performClick()
        assertTrue(existe("Ingresa el nombre del hábito"))
        assertTrue(existe("Selecciona una categoría"))
    }

    @Test
    fun crear_diasEspecificosPideAlMenosUnDia() {
        abrir("crear")
        prueba.onNodeWithText("Días específicos").performClick()
        prueba.onNodeWithText("Lun").assertIsDisplayed()
        prueba.onNodeWithText("Guardar hábito").performScrollTo().performClick()
        assertTrue(existe("Selecciona al menos un día"))
    }

    @Test
    fun crear_xPorSemanaMuestraElCampoDeVeces() {
        abrir("crear")
        prueba.onNodeWithText("X por semana").performClick()
        assertTrue(existe("Veces por semana (1 a 7)"))
    }

    ////////////////// Calendario (Stats)

    @Test
    fun calendario_muestraHabitosRachasYMes() {
        abrir("inicio")
        prueba.onNodeWithText("Stats").performClick()
        prueba.onNodeWithText("Progreso").assertIsDisplayed()
        assertTrue(existe("Hábitos activos"))
        assertTrue(existe("Hábitos archivados"))
        prueba.onNodeWithText("Racha actual").performScrollTo().assertIsDisplayed()
        assertTrue(existe("Mejor racha"))   // sale en el resumen y en estadísticas
        prueba.onNodeWithText(nombreDelMes(YearMonth.now())).performScrollTo().assertIsDisplayed()
    }

    ////////////////// Perfil

    @Test
    fun perfil_muestraUsuarioYArchivados() {
        abrir("inicio")
        prueba.onNodeWithText("Perfil").performClick()
        prueba.onNodeWithText("Ana Pérez").assertIsDisplayed()
        prueba.onNodeWithText("ana@gmail.com").assertIsDisplayed()
        prueba.onNodeWithText("Correr").assertIsDisplayed()
    }

    @Test
    fun perfil_cerrarSesionVuelveAlLogin() {
        abrir("perfil")
        prueba.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        prueba.onNodeWithText("Bienvenido de vuelta").assertIsDisplayed()
    }

    ////////////////// Logros (pantalla de práctica)

    @Test
    fun logros_abreSinErrores() {
        abrir("inicio")
        prueba.onNodeWithText("Logros").performClick()
        assertTrue(existe("Hábitos archivados"))
    }

    ////////////////// Puntos y niveles (requerimiento 3 / 15)

    @Test
    fun inicio_chipMuestraElNivelSegunLosPuntos() {
        abrir("inicio")
        // 250 puntos = nivel 3
        prueba.onNodeWithText("Nv. 3").assertIsDisplayed()
    }

    @Test
    fun perfil_muestraNivelYPuntos() {
        abrir("perfil")
        prueba.onNodeWithText("Nivel 3").assertIsDisplayed()
        prueba.onNodeWithText("250 pts").assertIsDisplayed()
        assertTrue(existe("Te faltan 50 puntos para el nivel 4. Ganas 10 puntos por cada hábito completado."))
    }

    ////////////////// Estadísticas y gráficos (requerimiento 2 / 22)

    @Test
    fun calendario_muestraEstadisticasYGraficos() {
        abrir("calendario/1")
        prueba.onNodeWithText("Estadísticas").performScrollTo().assertIsDisplayed()
        prueba.onNodeWithText("Cumplimiento").performScrollTo().assertIsDisplayed()
        prueba.onNodeWithText("Veces completado").performScrollTo().assertIsDisplayed()
        prueba.onNodeWithText("Por semana (últimas 8)").performScrollTo().assertIsDisplayed()
        prueba.onNodeWithText("Por mes (últimos 6)").performScrollTo().assertIsDisplayed()
    }
}
