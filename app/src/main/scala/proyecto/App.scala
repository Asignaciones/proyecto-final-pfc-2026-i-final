package proyecto

import AsignacionAulas._
import AsignacionAulasPar._
import org.scalameter._

object App {

  def main(args: Array[String]): Unit = {

    // ── Verificación con los ejemplos del enunciado ──────────────────────────

    val C1: Cursos = Vector(
      ("M01", 4, 8, 25), ("M02", 6, 10, 30), ("M03", 12, 16, 20)
    )
    val A1: Aulas       = Vector(("E101", 30), ("E102", 40))
    val DA1: Distancias = Vector(Vector(0, 3), Vector(3, 0))
    val w: Pesos        = (1000, 100, 1, 2)

    println("=== Verificación Ejemplo 1 ===")
    println(s"CT(α1=[0,0,1]) = ${costoAsignacion(C1, A1, DA1, Vector(0,0,1), w)}  (esperado 1031)")
    println(s"CT(α2=[0,1,0]) = ${costoAsignacion(C1, A1, DA1, Vector(0,1,0), w)}  (esperado 37)")
    val (opt, copt) = asignacionOptima(C1, A1, DA1, w)
    println(s"Óptima secuencial = $opt  costo = $copt")
    val (optP, coptP) = asignacionOptimaPar(C1, A1, DA1, w)
    println(s"Óptima paralela   = $optP  costo = $coptP")

    // ── Benchmarks ────────────────────────────────────────────────────────────

    println("\n=== Benchmarks asignacionOptima vs asignacionOptimaPar ===")
    println(f"${"n"}%4s  ${"m"}%4s  ${"m^n"}%10s  ${"Seq (ms)"}%12s  ${"Par (ms)"}%12s  ${"Aceleración(%)"}%16s")

    val instancias = List((4,3), (5,3), (6,4), (7,4), (8,5))

    instancias.foreach { case (n, m) =>
      val cursos = cursosAlAzar(n)
      val aulas  = aulasAlAzar(m)
      val d      = distanciasAlAzar(m)

      val tSeq = withWarmer(new Warmer.Default) measure {
        asignacionOptima(cursos, aulas, d, w)
      }
      val tPar = withWarmer(new Warmer.Default) measure {
        asignacionOptimaPar(cursos, aulas, d, w)
      }

      val tsMs = tSeq.value
      val tpMs = tPar.value
      val acel  = if (tsMs == 0) 0.0 else (tsMs - tpMs) / tsMs * 100.0
      val espacio = math.pow(m, n).toLong
      println(f"$n%4d  $m%4d  $espacio%10d  $tsMs%12.2f  $tpMs%12.2f  $acel%16.2f")
    }
  }
}