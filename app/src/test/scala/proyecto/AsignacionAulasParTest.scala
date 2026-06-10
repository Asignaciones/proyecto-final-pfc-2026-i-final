package proyecto

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import AsignacionAulas._
import AsignacionAulasPar._

@RunWith(classOf[JUnitRunner])
class AsignacionAulasParTest extends AnyFunSuite {

  // ---------------------------------------------------------------------------
  // Datos de los ejemplos del enunciado
  // ---------------------------------------------------------------------------

  val c1: Cursos     = Vector(("M01", 4, 8, 25), ("M02", 6, 10, 30), ("M03", 12, 16, 20))
  val a1: Aulas      = Vector(("E101", 30), ("E102", 40))
  val d1: Distancias = Vector(Vector(0, 3), Vector(3, 0))
  val w: Pesos       = (1000, 100, 1, 2)

  val c2: Cursos     = Vector(("F01", 0, 4, 40), ("F02", 4, 8, 25), ("F03", 8, 12, 50), ("F04", 12, 16, 15))
  val a2: Aulas      = Vector(("S201", 45), ("S202", 30))
  val d2: Distancias = Vector(Vector(0, 5), Vector(5, 0))

  // ---------------------------------------------------------------------------
  // choquesPar (5 casos)
  // ---------------------------------------------------------------------------

  test("choquesPar: asignacion [0,0,1] tiene 1 choque") {
    assert(choquesPar(c1, Vector(0, 0, 1)) == 1)
  }

  test("choquesPar: asignacion [0,1,0] no tiene choques") {
    assert(choquesPar(c1, Vector(0, 1, 0)) == 0)
  }

  test("choquesPar: coincide con choques en asignacion [0,0,0]") {
    val a = Vector(0, 0, 0)
    assert(choquesPar(c1, a) == choques(c1, a))
  }

  test("choquesPar: tres cursos solapados en la misma aula producen 3 choques") {
    val cursos: Cursos = Vector(("A", 0, 6, 10), ("B", 2, 8, 10), ("C", 4, 10, 10))
    assert(choquesPar(cursos, Vector(0, 0, 0)) == 3)
  }

  test("choquesPar: ejemplo 2 asignacion [0,1,0,1] coincide con choques") {
    val a = Vector(0, 1, 0, 1)
    assert(choquesPar(c2, a) == choques(c2, a))
  }

  // ---------------------------------------------------------------------------
  // desperdicioPar (5 casos)
  // ---------------------------------------------------------------------------

  test("desperdicioPar: asignacion [0,0,1] tiene desperdicio 25") {
    assert(desperdicioPar(c1, a1, Vector(0, 0, 1)) == 25)
  }

  test("desperdicioPar: asignacion [0,1,0] coincide con desperdicio secuencial") {
    val a = Vector(0, 1, 0)
    assert(desperdicioPar(c1, a1, a) == desperdicio(c1, a1, a))
  }

  test("desperdicioPar: ejemplo 2 asignacion [0,1,0,1] coincide con desperdicio secuencial") {
    val a = Vector(0, 1, 0, 1)
    assert(desperdicioPar(c2, a2, a) == desperdicio(c2, a2, a))
  }

  test("desperdicioPar: aulas grandes coincide con desperdicio secuencial") {
    val aulas: Aulas = Vector(("G", 100))
    val a = Vector(0, 0, 0)
    assert(desperdicioPar(c1, aulas, a) == desperdicio(c1, aulas, a))
  }

  test("desperdicioPar: aulas con capacidad exacta produce desperdicio minimo") {
    val aulas: Aulas = Vector(("X", 25), ("Y", 30))
    val a = Vector(0, 1, 0)
    assert(desperdicioPar(c1, aulas, a) == desperdicio(c1, aulas, a))
  }

  // ---------------------------------------------------------------------------
  // movilidadPar (5 casos)
  // ---------------------------------------------------------------------------

  test("movilidadPar: asignacion [0,0,1] tiene movilidad 3") {
    assert(movilidadPar(c1, a1, d1, Vector(0, 0, 1)) == 3)
  }

  test("movilidadPar: asignacion [0,1,0] coincide con movilidad secuencial") {
    val a = Vector(0, 1, 0)
    assert(movilidadPar(c1, a1, d1, a) == movilidad(c1, a1, d1, a))
  }

  test("movilidadPar: todos en la misma aula tiene movilidad 0") {
    assert(movilidadPar(c1, a1, d1, Vector(0, 0, 0)) == 0)
  }

  test("movilidadPar: ejemplo 2 asignacion [0,1,0,1] coincide con movilidad secuencial") {
    val a = Vector(0, 1, 0, 1)
    assert(movilidadPar(c2, a2, d2, a) == movilidad(c2, a2, d2, a))
  }

  test("movilidadPar: un solo curso tiene movilidad 0") {
    val cursos: Cursos = Vector(("X", 0, 4, 10))
    val aulas: Aulas   = Vector(("A", 20))
    val d: Distancias  = Vector(Vector(0))
    assert(movilidadPar(cursos, aulas, d, Vector(0)) == 0)
  }

  // ---------------------------------------------------------------------------
  // generarAsignacionesPar (5 casos)
  // ---------------------------------------------------------------------------

  test("generarAsignacionesPar: 2 cursos y 2 aulas produce 4 asignaciones") {
    assert(generarAsignacionesPar(2, 2).length == 4)
  }

  test("generarAsignacionesPar: 3 cursos y 3 aulas produce 27 asignaciones") {
    assert(generarAsignacionesPar(3, 3).length == 27)
  }

  test("generarAsignacionesPar: contiene las mismas asignaciones que la version secuencial") {
    assert(generarAsignacionesPar(2, 3).toSet == generarAsignaciones(2, 3).toSet)
  }

  test("generarAsignacionesPar: 0 cursos produce 1 asignacion vacia") {
    assert(generarAsignacionesPar(0, 3) == Vector(Vector()))
  }

  test("generarAsignacionesPar: no hay asignaciones duplicadas para n=2, m=3") {
    val res = generarAsignacionesPar(2, 3)
    assert(res.distinct.length == res.length)
  }

  // ---------------------------------------------------------------------------
  // asignacionOptimaPar (5 casos)
  // ---------------------------------------------------------------------------

  test("asignacionOptimaPar: el costo de la optima no supera el de [0,1,0] (37)") {
    val (_, costo) = asignacionOptimaPar(c1, a1, d1, w)
    assert(costo <= 31)
  }

  test("asignacionOptimaPar: el costo coincide con el de la version secuencial") {
    val (_, costoSeq) = asignacionOptima(c1, a1, d1, w)
    val (_, costoPar) = asignacionOptimaPar(c1, a1, d1, w)
    assert(costoSeq == costoPar)
  }

  test("asignacionOptimaPar: el costo calculado coincide con costoAsignacion") {
    val (opt, costo) = asignacionOptimaPar(c1, a1, d1, w)
    assert(costo == costoAsignacion(c1, a1, d1, opt, w))
  }

  test("asignacionOptimaPar: la asignacion tiene exactamente n elementos") {
    val (opt, _) = asignacionOptimaPar(c1, a1, d1, w)
    assert(opt.length == c1.length)
  }

  test("asignacionOptimaPar: ejemplo 2 tiene costo no mayor al de [0,1,0,1] (155)") {
    val (_, costo) = asignacionOptimaPar(c2, a2, d2, w)
    assert(costo <= 155)
  }
}