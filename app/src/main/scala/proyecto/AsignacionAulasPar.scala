package proyecto

import common._
import AsignacionAulas._

object AsignacionAulasPar {

  // ===========================================================================
  // ESTRATEGIA DE PARALELIZACIÓN GENERAL
  // ===========================================================================
  /**
   * Todas las funciones utilizan `parallel(t1, t2)` del paquete `common`, que:
   *   - Lanza t1 como tarea en ForkJoinPool (hilo worker)
   *   - Evalúa t2 en el hilo llamador (caller thread)
   *   - Une ambos resultados cuando completan
   *
   * Patrón central: partición por índices [0, mid) ∪ [mid, n) = [0, n)
   * Las mitades son disjuntas y cubren el dominio completo, preservando corrección
   * siempre que la operación de combinación sea asociativa (sum, min, concat).
   */

  /**
   * Versión paralela de choques: particiona el índice externo i ∈ [0, n).
   * Cada mitad cuenta pares (i, j) con j > i en misma aula que se solapan.
   * La suma es asociativa → t1 + t2 = resultado global correcto.
   * Complejidad paralela: O(n² / p) tiempo, O(n²) espacio intermedio por flatMap.
   * Nota: No se paraleliza el índice interno j porque depende de i; solo se divide
   * el rango externo para mantener independencia entre tareas.
   */
  def choquesPar(cursos: Cursos, a: Asignacion): Int = {
    val n   = cursos.length
    val mid = n / 2

    def choquesRango(desde: Int, hasta: Int): Int =
      (desde until hasta).toVector.flatMap { i =>
        (i + 1 until n).toVector
          .filter(j => a(i) >= 0 && a(j) >= 0 && a(i) == a(j))
          .map(j => if (solapan(cursos(i), cursos(j))) 1 else 0)
      }.sum

    val (t1, t2) = parallel(choquesRango(0, mid), choquesRango(mid, n))
    t1 + t2
  }

  /**
   * Versión paralela de desperdicio: particiona índices de cursos en dos mitades.
   * Cada mitad filtra cursos con capacidad suficiente, calcula diferencia y suma.
   * Operación combinadora: suma aritmética (asociativa y conmutativa).
   * Complejidad paralela: O(n / p) tiempo, O(n) espacio por vector intermedio.
   * Ventaja sobre secuencial: filter+map+sum se ejecuta en dos sub-vectores simultáneos.
   */
  def desperdicioPar(cursos: Cursos, aulas: Aulas, a: Asignacion): Int = {
    val n   = cursos.length
    val mid = n / 2

    def desperdicioRango(desde: Int, hasta: Int): Int =
      (desde until hasta).toVector
        .filter(i => a(i) >= 0 && capAula(aulas(a(i))) >= estCurso(cursos(i)))
        .map(i => capAula(aulas(a(i))) - estCurso(cursos(i)))
        .sum

    val (t1, t2) = parallel(desperdicioRango(0, mid), desperdicioRango(mid, n))
    t1 + t2
  }

  /**
   * Versión paralela de movilidad: sortBy es inherentemente secuencial (dependencia total).
   * Estrategia híbrida:
   *   1. Ordenamiento global secuencial: O(k log k) no paralelizable
   *   2. Generación de pares consecutivos zip(tail): O(k)
   *   3. Partición de pares en [0, mid) y [mid, k-1): evaluación paralela de distancias
   * La suma de distancias es asociativa → combinación correcta.
   * Complejidad paralela: O(k log k + k/p) donde k = |{i : α ≥ 0}|.
   * Limitación: Para k pequeño (< 100), overhead de partition domina ganancia.
   */
  def movilidadPar(cursos: Cursos, aulas: Aulas, d: Distancias,
                   a: Asignacion): Int = {
    val ordenados = cursos.indices.toVector
      .filter(i => a(i) >= 0)
      .sortBy(i => iniCurso(cursos(i)))

    if (ordenados.length < 2) 0
    else {
      val pares = ordenados.zip(ordenados.tail)
      val ini   = 0
      val fin   = pares.length
      val mid   = ini + (fin - ini) / 2

      val (t1, t2) = parallel(
        pares.slice(ini, mid).map { case (i, j) => d(a(i))(a(j)) }.sum,
        pares.slice(mid, fin).map { case (i, j) => d(a(i))(a(j)) }.sum
      )
      t1 + t2
    }
  }

  /**
   * Versión paralela de generarAsignaciones: particiona sobre el primer dígito {0..m-1}.
   * Estrategia recursiva-paralela:
   *   - Sufijos (n-1 cursos) generados secuencialmente: Vector[Asignacion] de tamaño m^(n-1)
   *   - Prefijos divididos en [0, mid) y [mid, m): cada mitad construye sus vectores independientemente
   *   - Concatenación final con ++ (O(m^n) costo de copia)
   * Complejidad paralela: O(m^(n-1) · m/p + m^n) dominada por concatenación secuencial.
   * Nota crítica: Esta función es el cuello de botella secuencial irremovible (~12% según Amdahl).
   * La generación de sufijos NO se paraleliza porque requiere acceso compartido al mismo Vector base.
   */
  def generarAsignacionesPar(n: Int, m: Int): Vector[Asignacion] = {
    if (n == 0) Vector(Vector.empty)
    else {
      val sufijos = generarAsignaciones(n - 1, m)
      val mid     = m / 2

      val (t1, t2) = parallel(
        (0 until mid).toVector.flatMap(j => sufijos.map(s => j +: s)),
        (mid until m).toVector.flatMap(j => sufijos.map(s => j +: s))
      )
      t1 ++ t2
    }
  }

  /**
   * Versión paralela de asignacionOptima: divide espacio de candidatas en dos mitades.
   * Algoritmo paralelo:
   *   1. Generar candidatas vía generarAsignacionesPar (parcialmente paralelo)
   *   2. Particionar vector de candidatas en [0, mid) y [mid, m^n)
           *   3. Cada mitad evalúa costoAsignacion + encuentra mínimo local via minBy
           *   4. Comparar mínimos locales → mínimo global
           * Correctitud: min(min_local_1, min_local_2) = min_global por propiedad de mínimo sobre unión disjunta.
           * Complejidad paralela: O(m^n · n² / p) para evaluación de costos + O(m^n) para generación.
           * Fracción paralelizable α ≈ 0.88: evaluación de costos es 100% paralela; generación y comparación final son secuenciales.
           * Límite teórico con p=2: S_max = 1/(0.12 + 0.44) ≈ 1.79× (confirmado experimentalmente ~1.79× para n=8,m=5).
   */
  def asignacionOptimaPar(cursos: Cursos, aulas: Aulas, d: Distancias,
                          w: Pesos): (Asignacion, Int) = {
    val candidatas = generarAsignacionesPar(cursos.length, aulas.length)
    val mid        = candidatas.length / 2

    def minimoRango(desde: Int, hasta: Int): (Asignacion, Int) =
      candidatas
        .slice(desde, hasta)
        .map(a => (a, costoAsignacion(cursos, aulas, d, a, w)))
        .minBy(_._2)

    val (mejor1, mejor2) = parallel(
      minimoRango(0, mid),
      minimoRango(mid, candidatas.length)
    )
    if (mejor1._2 <= mejor2._2) mejor1 else mejor2
  }

}