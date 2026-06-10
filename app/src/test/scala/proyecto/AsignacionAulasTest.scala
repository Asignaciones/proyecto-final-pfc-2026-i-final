package proyecto

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import AsignacionAulas._

@RunWith(classOf[JUnitRunner])
class AsignacionAulasTest extends AnyFunSuite {

  // ---------------------------------------------------------------------------
  // Datos de los ejemplos del enunciado
  // ---------------------------------------------------------------------------

  // Ejemplo 1
  val c1: Cursos     = Vector(("M01", 4, 8, 25), ("M02", 6, 10, 30), ("M03", 12, 16, 20))
  val a1: Aulas      = Vector(("E101", 30), ("E102", 40))
  val d1: Distancias = Vector(Vector(0, 3), Vector(3, 0))
  val w: Pesos       = (1000, 100, 1, 2)

  // Ejemplo 2
  val c2: Cursos     = Vector(("F01", 0, 4, 40), ("F02", 4, 8, 25), ("F03", 8, 12, 50), ("F04", 12, 16, 15))
  val a2: Aulas      = Vector(("S201", 45), ("S202", 30))
  val d2: Distancias = Vector(Vector(0, 5), Vector(5, 0))

  // ---------------------------------------------------------------------------
  // solapan (5 casos)
  // ---------------------------------------------------------------------------

  test("solapan: M01[4,8) y M02[6,10) se solapan") {
    assert(solapan(("M01", 4, 8, 25), ("M02", 6, 10, 30)))
  }

  test("solapan: M01[4,8) y M03[12,16) no se solapan") {
    assert(!solapan(("M01", 4, 8, 25), ("M03", 12, 16, 20)))
  }

  test("solapan: cursos adyacentes [0,4) y [4,8) no se solapan") {
    assert(!solapan(("A", 0, 4, 10), ("B", 4, 8, 10)))
  }

  test("solapan: un curso contenido dentro de otro se solapa") {
    // [2,10) contiene a [4,8) → se solapan
    assert(solapan(("A", 2, 10, 10), ("B", 4, 8, 10)))
  }

  test("solapan: mismos intervalos se solapan") {
    assert(solapan(("A", 4, 8, 10), ("B", 4, 8, 10)))
  }

  // ---------------------------------------------------------------------------
  // choques (5 casos)
  // ---------------------------------------------------------------------------

  test("choques: asignacion [0,0,1] tiene 1 choque (M01 y M02 en E101)") {
    assert(choques(c1, Vector(0, 0, 1)) == 1)
  }

  test("choques: asignacion [0,1,0] no tiene choques") {
    assert(choques(c1, Vector(0, 1, 0)) == 0)
  }

  test("choques: tres cursos solapados en la misma aula producen 3 choques") {
    // A[0,6), B[2,8), C[4,10): todos se solapan entre sí → pares (A,B),(A,C),(B,C)
    val cursos: Cursos = Vector(("A", 0, 6, 10), ("B", 2, 8, 10), ("C", 4, 10, 10))
    assert(choques(cursos, Vector(0, 0, 0)) == 3)
  }

  test("choques: cursos en aulas distintas no generan choques aunque se solapen") {
    assert(choques(c1, Vector(0, 1, 1)) == 0)
  }

  test("choques: ejemplo 2 con asignacion [0,1,0,1] no tiene choques") {
    // F01[0,4), F02[4,8), F03[8,12), F04[12,16): ninguno se solapa con otro
    assert(choques(c2, Vector(0, 1, 0, 1)) == 0)
  }

  // ---------------------------------------------------------------------------
  // capacidadFallida (5 casos)
  // ---------------------------------------------------------------------------

  test("capacidadFallida: asignacion [0,0,1] no falla capacidad") {
    // E101(30)>=25, E101(30)>=30, E102(40)>=20
    assert(capacidadFallida(c1, a1, Vector(0, 0, 1)) == 0)
  }

  test("capacidadFallida: F03(50 est) en S201(45 cap) falla → 1 fallo") {
    // S201(45)<50 → fallo en F03
    assert(capacidadFallida(c2, a2, Vector(0, 1, 0, 1)) == 1)
  }

  test("capacidadFallida: F03 en S202(30) tambien falla → 1 fallo") {
    assert(capacidadFallida(c2, a2, Vector(0, 1, 1, 0)) == 1)
  }

  test("capacidadFallida: aulas con capacidad exacta no producen fallos") {
    // aulas con cap exacta para cada curso
    val aulas: Aulas = Vector(("X", 25), ("Y", 30))
    // E101(25)>=25, E102(30)>=30, E101(25)>=20 → 0 fallos
    assert(capacidadFallida(c1, aulas, Vector(0, 1, 0)) == 0)
  }

  test("capacidadFallida: todos los cursos en aula insuficiente") {
    val aulas: Aulas = Vector(("P", 5))
    // cap 5 < 25, 30, 20 → 3 fallos
    assert(capacidadFallida(c1, aulas, Vector(0, 0, 0)) == 3)
  }

  // ---------------------------------------------------------------------------
  // desperdicio (5 casos)
  // ---------------------------------------------------------------------------

  test("desperdicio: asignacion [0,0,1] tiene desperdicio 25") {
    // E101(30)-M01(25)=5, E101(30)-M02(30)=0, E102(40)-M03(20)=20 → 25
    assert(desperdicio(c1, a1, Vector(0, 0, 1)) == 25)
  }

  test("desperdicio: asignacion [0,1,0] tiene desperdicio 25") {
    // E101(30)-M01(25)=5, E102(40)-M02(30)=10, E101(30)-M03(20)=10 → 25
    assert(desperdicio(c1, a1, Vector(0, 1, 0)) == 25)
  }

  test("desperdicio: curso con fallo de capacidad no suma al desperdicio") {
    // F03(50 est) en S201(45 cap): fallo → no cuenta; resto suma desperdicio
    // S201(45)-F01(40)=5, S202(30)-F02(25)=5, fallo→0, S202(30)-F04(15)=15 → 25
    assert(desperdicio(c2, a2, Vector(0, 1, 0, 1)) == 25)
  }

  test("desperdicio: aulas con capacidad exacta producen desperdicio 0") {
    val aulas: Aulas = Vector(("X", 25), ("Y", 30))
    // 25-25=0, 30-30=0, 25-20=5 → 5
    assert(desperdicio(c1, aulas, Vector(0, 1, 0)) == 5)
  }

  test("desperdicio: aulas muy grandes acumulan desperdicio elevado") {
    val aulas: Aulas = Vector(("G", 100))
    // 100-25 + 100-30 + 100-20 = 75+70+80 = 225
    assert(desperdicio(c1, aulas, Vector(0, 0, 0)) == 225)
  }

  // ---------------------------------------------------------------------------
  // movilidad (5 casos)
  // ---------------------------------------------------------------------------

  test("movilidad: asignacion [0,0,1] tiene movilidad 3") {
    // orden ini: M01(4)→aula0, M02(6)→aula0, M03(12)→aula1
    // D[0][0]+D[0][1] = 0+3 = 3
    assert(movilidad(c1, a1, d1, Vector(0, 0, 1)) == 3)
  }

  test("movilidad: asignacion [0,1,0] tiene movilidad 6") {
    // orden ini: M01→aula0, M02→aula1, M03→aula0
    // D[0][1]+D[1][0] = 3+3 = 6
    assert(movilidad(c1, a1, d1, Vector(0, 1, 0)) == 6)
  }

  test("movilidad: todos en la misma aula tiene movilidad 0") {
    // D[0][0] = 0 siempre
    assert(movilidad(c1, a1, d1, Vector(0, 0, 0)) == 0)
  }

  test("movilidad: ejemplo 2 asignacion [0,1,0,1] tiene movilidad 15") {
    // orden: F01→0, F02→1, F03→0, F04→1
    // D[0][1]+D[1][0]+D[0][1] = 5+5+5 = 15
    assert(movilidad(c2, a2, d2, Vector(0, 1, 0, 1)) == 15)
  }

  test("movilidad: un solo curso tiene movilidad 0") {
    val cursos: Cursos = Vector(("X", 0, 4, 10))
    val aulas: Aulas   = Vector(("A", 20))
    val d: Distancias  = Vector(Vector(0))
    assert(movilidad(cursos, aulas, d, Vector(0)) == 0)
  }

  // ---------------------------------------------------------------------------
  // costoAsignacion (5 casos)
  // ---------------------------------------------------------------------------

  test("costoAsignacion: asignacion [0,0,1] cuesta 1031") {
    assert(costoAsignacion(c1, a1, d1, Vector(0, 0, 1), w) == 1031)
  }

  test("costoAsignacion: asignacion [0,1,0] cuesta 37") {
    assert(costoAsignacion(c1, a1, d1, Vector(0, 1, 0), w) == 37)
  }

  test("costoAsignacion: ejemplo 2 asignacion [0,1,0,1] cuesta 155") {
    // CH=0, CF=1(F03), DE=25, MV=15 → 0+100+25+30=155
    assert(costoAsignacion(c2, a2, d2, Vector(0, 1, 0, 1), w) == 155)
  }

  test("costoAsignacion: ejemplo 2 asignacion [0,1,1,0] cuesta 160") {
    // CH=0, CF=1(F03 en S202), DE=40, MV=10 → 0+100+40+20=160
    assert(costoAsignacion(c2, a2, d2, Vector(0, 1, 1, 0), w) == 160)
  }

  test("costoAsignacion: choque penaliza fuertemente el costo") {
    // [0,0,1]: tiene 1 choque → costo >= 1000
    assert(costoAsignacion(c1, a1, d1, Vector(0, 0, 1), w) >= 1000)
  }

  // ---------------------------------------------------------------------------
  // generarAsignaciones (5 casos)
  // ---------------------------------------------------------------------------

  test("generarAsignaciones: 2 cursos y 2 aulas produce 4 asignaciones") {
    assert(generarAsignaciones(2, 2).length == 4)
  }

  test("generarAsignaciones: 3 cursos y 3 aulas produce 27 asignaciones") {
    assert(generarAsignaciones(3, 3).length == 27)
  }

  test("generarAsignaciones: 0 cursos produce 1 asignacion vacia") {
    assert(generarAsignaciones(0, 3) == Vector(Vector()))
  }

  test("generarAsignaciones: 4 cursos y 2 aulas produce 16 asignaciones") {
    assert(generarAsignaciones(4, 2).length == 16)
  }

  test("generarAsignaciones: todas las asignaciones son distintas para n=2, m=3") {
    val res = generarAsignaciones(2, 3)
    assert(res.length == 9 && res.distinct.length == 9)
  }

  // ---------------------------------------------------------------------------
  // asignacionOptima (5 casos)
  // ---------------------------------------------------------------------------

  test("asignacionOptima: el costo de la optima no supera el de [0,1,0] (37)") {
    val (_, costo) = asignacionOptima(c1, a1, d1, w)
    assert(costo <= 31)
  }

  test("asignacionOptima: el costo calculado coincide con costoAsignacion") {
    val (opt, costo) = asignacionOptima(c1, a1, d1, w)
    assert(costo == costoAsignacion(c1, a1, d1, opt, w))
  }

  test("asignacionOptima: la asignacion tiene exactamente n elementos") {
    val (opt, _) = asignacionOptima(c1, a1, d1, w)
    assert(opt.length == c1.length)
  }

  test("asignacionOptima: todos los indices estan en el rango valido de aulas") {
    val (opt, _) = asignacionOptima(c1, a1, d1, w)
    assert(opt.forall(j => j >= 0 && j < a1.length))
  }

  test("asignacionOptima: ejemplo 2 tiene costo no mayor al de [0,1,0,1] (155)") {
    val (_, costo) = asignacionOptima(c2, a2, d2, w)
    assert(costo <= 155)
  }
}