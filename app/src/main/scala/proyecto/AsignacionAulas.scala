package proyecto

import scala.util.Random

object AsignacionAulas {

  // ===========================================================================
  // DEFINICIÓN DE TIPOS Y DOMINIO DEL PROBLEMA
  // ===========================================================================

  /**
   * Representa un curso académico.
   * @param _1 Identificador único del curso (ej: "M01")
   * @param _2 Hora de inicio en bloques de 30 min desde las 6:00 AM (4 = 8:00 AM)
   * @param _3 Hora de fin en bloques de 30 min (intervalo semiabierto [ini, fin))
   * @param _4 Número de estudiantes matriculados
   */
  type Curso = (String, Int, Int, Int)
  type Cursos = Vector[Curso]

  /**
   * Representa un aula física.
   * @param _1 Identificador del aula (ej: "E101")
   * @param _2 Capacidad máxima de estudiantes
   */
  type Aula = (String, Int)
  type Aulas = Vector[Aula]

  /**
   * Vector de asignación donde asignacion(i) = j indica que el curso i
   * se dicta en el aula j. El valor -1 representa una asignación nula o fallida.
   */
  type Asignacion = Vector[Int]

  /** Matriz simétrica de distancias entre aulas. d(i)(j) = distancia(aula_i, aula_j). */
  type Distancias = Vector[Vector[Int]]

  /** Pesos para la función objetivo: (w_choques, w_capacidadFallida, w_desperdicio, w_movilidad). */
  type Pesos = (Int, Int, Int, Int)

  // ===========================================================================
  // GENERADORES DE DATOS ALEATORIOS (FIXTURES PARA BENCHMARKING)
  // ===========================================================================

  val random: Random = new Random()

  /**
   * Genera n cursos con horarios y demandas aleatorias.
   * - Horarios: inicio uniforme en [0, 29), duración en [2, 8] bloques.
   * - Demanda: estudiantes uniformes en [5, 50].
   * Complejidad: O(n)
   */
  def cursosAlAzar(n: Int): Cursos =
    Vector.tabulate(n) { i =>
      val ini = random.nextInt(29)
      val dur = random.nextInt(7) + 2
      ("C" + i, ini, ini + dur, random.nextInt(46) + 5)
    }

  /**
   * Genera m aulas con capacidades aleatorias en [15, 60].
   * Complejidad: O(m)
   */
  def aulasAlAzar(m: Int): Aulas =
    Vector.tabulate(m)(j => ("E" + j, random.nextInt(46) + 15))

  /**
   * Genera una matriz de distancias simétrica con diagonal cero.
   * Valores fuera de diagonal: uniformes en [1, 2m].
   * Garantiza la propiedad métrica básica: d(i,j) = d(j,i) y d(i,i) = 0.
   * Complejidad: O(m²)
   */
  def distanciasAlAzar(m: Int): Distancias = {
    val v = Vector.fill(m, m)(random.nextInt(m * 2) + 1)
    Vector.tabulate(m, m)((i, j) =>
      if (i < j) v(i)(j)
      else if (i == j) 0
      else v(j)(i)
    )
  }

  // ===========================================================================
  // FUNCIONES DE ACCESO (SELECTORES INMUTABLES)
  // ===========================================================================

  def idCurso(c: Curso): String = c._1
  def iniCurso(c: Curso): Int = c._2
  def finCurso(c: Curso): Int = c._3
  def estCurso(c: Curso): Int = c._4
  def idAula(a: Aula): String = a._1
  def capAula(a: Aula): Int = a._2

  // ===========================================================================
  // LÓGICA DE NEGOCIO SECUENCIAL
  // ===========================================================================

  /**
   * Determina si dos intervalos semiabiertos [ini₁, fin₁) y [ini₂, fin₂) se intersectan.
   * Condición matemática: ini₁ < fin₂ ∧ ini₂ < fin₁
   * Esta condición es necesaria y suficiente para intersección no vacía de intervalos semiabiertos.
   * Complejidad: O(1)
   */
  def solapan(c1: Curso, c2: Curso): Boolean =
    iniCurso(c1) < finCurso(c2) && iniCurso(c2) < finCurso(c1)

  /**
   * Cuenta el número de pares (i, j) con i < j tales que:
   *   1. Ambos cursos están asignados al mismo aula (αᵢ = αⱼ ≥ 0)
   *   2. Sus intervalos temporales se solapan
   * Implementación funcional: flatMap sobre índices externos genera todos los pares válidos;
   * filter elimina pares sin conflicto de aula; map convierte solapamiento a 1/0; sum acumula.
   * Equivale a: Σᵢ₌₀ⁿ⁻¹ Σⱼ₌ᵢ₊₁ⁿ⁻¹ [α=αⱼ≥0 ∧ solapan(cᵢ,cⱼ)]
   * Complejidad: O(n²) en tiempo y espacio intermedio por flatMap.
   */
  def choques(cursos: Cursos, a: Asignacion): Int =
    cursos.indices.toVector.flatMap { i =>
      cursos.indices.toVector
        .filter(j => j > i && a(i) >= 0 && a(j) >= 0 && a(i) == a(j))
        .map(j => if (solapan(cursos(i), cursos(j))) 1 else 0)
    }.sum

  /**
   * Cuenta cuántos cursos asignados tienen demanda mayor que la capacidad del aula asignada.
   * Filtra índices i donde αᵢ ≥ 0 ∧ cap(A[αᵢ]) < est(cᵢ) y retorna la cardinalidad.
   * Complejidad: O(n)
   */
  def capacidadFallida(cursos: Cursos, aulas: Aulas, a: Asignacion): Int =
    cursos.indices.toVector
      .filter(i => a(i) >= 0 && capAula(aulas(a(i))) < estCurso(cursos(i)))
      .length

  /**
   * Calcula el desperdicio total de capacidad: suma de (cap - est) para cursos
   * cuya aula asignada tiene capacidad suficiente. Los cursos con fallo de capacidad
   * contribuyen 0 (equivalente a max(cap - est, 0)).
   * Estrategia: filter descarta fallos → map calcula diferencia positiva → sum acumula.
   * Complejidad: O(n)
   */
  def desperdicio(cursos: Cursos, aulas: Aulas, a: Asignacion): Int =
    cursos.indices.toVector
      .filter(i => a(i) >= 0 && capAula(aulas(a(i))) >= estCurso(cursos(i)))
      .map(i => capAula(aulas(a(i))) - estCurso(cursos(i)))
      .sum

  /**
   * Calcula la movilidad total: suma de distancias entre aulas de cursos consecutivos
   * cuando se ordenan por hora de inicio.
   * Algoritmo:
   *   1. Filtrar cursos asignados (αᵢ ≥ 0)
   *   2. Ordenar por iniCurso → permutación σ
   *   3. zip(tail) genera pares consecutivos (σⱼ, σⱼ₁)
   *   4. Mapear a distancia D[ασⱼ][ασⱼ₊₁] y sumar
   * Equivale a: Σⱼ₌₀ᵏ⁻² D[α_{σⱼ}][α_{σⱼ₊₁}] donde k = |{i : αᵢ ≥ 0}|
   * Complejidad: O(k log k) por sortBy, O(k) para zip+map+sum.
   */
  def movilidad(cursos: Cursos, aulas: Aulas, d: Distancias, a: Asignacion): Int = {
    val ordenados = cursos.indices.toVector
      .filter(i => a(i) >= 0)
      .sortBy(i => iniCurso(cursos(i)))
    if (ordenados.length < 2) 0
    else
      ordenados.zip(ordenados.tail)
        .map { case (i, j) => d(a(i))(a(j)) }
        .sum
  }

  /**
   * Función objetivo ponderada: CT(α) = w_CH·CH + w_CF·CF + w_DE·DE + w_MV·MV
   * Combinación lineal de las cuatro métricas de calidad. La corrección depende
   * exclusivamente de la corrección de los componentes individuales y la aritmética entera.
   * Complejidad: O(n²) dominada por choques.
   */
  def costoAsignacion(cursos: Cursos, aulas: Aulas, d: Distancias, a: Asignacion, w: Pesos): Int = {
    val (wCH, wCF, wDE, wMV) = w
    wCH * choques(cursos, a) +
      wCF * capacidadFallida(cursos, aulas, a) +
      wDE * desperdicio(cursos, aulas, a) +
      wMV * movilidad(cursos, aulas, d, a)
  }

  /**
   * Genera exhaustivamente todas las asignaciones posibles en {0, ..., m-1}ⁿ.
   * Recursión estructural sobre n:
   *   - Caso base (n=0): Vector(Vector.empty) ≡ {0..m-1}
   *   - Paso inductivo: gen(n-1,m).flatMap(asig → (0 until m).map(j → asig :+ j))
   *     Extiende cada asignación de longitud n-1 con cada valor de aula posible.
   * Cardinalidad del resultado: m. Espacio: O(n · mⁿ).
   * Nota: Esta función es el cuello de botella secuencial irremovible en la versión paralela.
   */
  def generarAsignaciones(n: Int, m: Int): Vector[Asignacion] =
    if (n == 0) Vector(Vector.empty)
    else
      generarAsignaciones(n - 1, m).flatMap(asig =>
        (0 until m).toVector.map(j => asig :+ j)
      )

  /**
   * Búsqueda exhaustiva de la asignación óptima por fuerza bruta.
   * Algoritmo:
   *   1. Generar espacio completo de candidatas: generarAsignaciones(n, m) → mⁿ elementos
   *   2. Evaluar costoAsignacion para cada candidata: map((a, CT(a)))
   *   3. Seleccionar mínimo global: minBy(_._2)
   * Correctitud garantizada por: exhaustividad de generarAsignaciones + corrección de costoAsignacion.
   * Complejidad temporal: O(mⁿ · n²). Complejidad espacial: O(m · n).
   * Límite práctico: viable hasta n≈8, m≈5 (~390K candidatas).
   */
  def asignacionOptima(cursos: Cursos, aulas: Aulas, d: Distancias, w: Pesos): (Asignacion, Int) =
    generarAsignaciones(cursos.length, aulas.length)
      .map(a => (a, costoAsignacion(cursos, aulas, d, a, w)))
      .minBy(_._2)
}