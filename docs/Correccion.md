# Informe de Corrección

**Fundamentos de Programación Funcional y Concurrente**  
**Integrantes:** [completar]

---

## Argumentación de corrección

### `solapan`

**Especificación:**

$$\text{solapan}(c_1, c_2) = \text{ini}_{c_1} < \text{fin}_{c_2} \;\land\; \text{ini}_{c_2} < \text{fin}_{c_1}$$

**Implementación:**

```scala
def solapan(c1: Curso, c2: Curso): Boolean =
  iniCurso(c1) < finCurso(c2) && iniCurso(c2) < finCurso(c1)
```

**Argumentación:** Dos intervalos semiabiertos $[a, b)$ y $[c, d)$ se intersectan si y solo si $a < d \land c < b$. La implementación reproduce fielmente esta condición. ✓

---

### `choques`

**Especificación:**

$$\text{CH}^\alpha_C = |\{(i,j) \mid 0 \leq i < j < n,\; \alpha_i = \alpha_j \geq 0,\; c_i \text{ solapa con } c_j\}|$$

**Implementación:**

```scala
def choques(cursos: Cursos, a: Asignacion): Int =
  cursos.indices.toVector.flatMap { i =>
    cursos.indices.toVector
      .filter(j => j > i && a(i) >= 0 && a(j) >= 0 && a(i) == a(j))
      .map(j => if (solapan(cursos(i), cursos(j))) 1 else 0)
  }.sum
```

**Argumentación:** Para cada $i \in [0, n)$, se filtran los $j > i$ con $\alpha_i = \alpha_j \geq 0$ y se mapea a 1 si $\text{solapan}(c_i, c_j)$, a 0 si no. La suma final es exactamente:

$$\sum_{i=0}^{n-1} \sum_{\substack{j = i+1 \\ \alpha_j = \alpha_i \geq 0}}^{n-1} [\text{solapan}(c_i, c_j)]$$

que coincide con la especificación. ✓

---

### `capacidadFallida`

**Especificación:**

$$\text{CF}^\alpha_{C,A} = |\{i \mid \alpha_i \geq 0,\; \text{cap}^A_{\alpha_i} < \text{est}^C_i\}|$$

**Implementación:**

```scala
def capacidadFallida(cursos: Cursos, aulas: Aulas, a: Asignacion): Int =
  cursos.indices.toVector
    .filter(i => a(i) >= 0 && capAula(aulas(a(i))) < estCurso(cursos(i)))
    .length
```

**Argumentación:** Se filtran exactamente los índices $i$ donde $\alpha_i \geq 0$ y $\text{cap}_{A[\alpha_i]} < \text{est}_{c_i}$, y se cuenta la longitud. Coincide con la especificación. ✓

---

### `desperdicio`

**Especificación:**

$$\text{DE}^\alpha_{C,A} = \sum_{\substack{i=0 \\ \alpha_i \geq 0}}^{n-1} \max\!\left(\text{cap}^A_{\alpha_i} - \text{est}^C_i,\; 0\right)$$

Cuando $\text{cap}^A_{\alpha_i} < \text{est}^C_i$ el término es $0$ (curso con fallo, no suma desperdicio).

**Implementación:**

```scala
def desperdicio(cursos: Cursos, aulas: Aulas, a: Asignacion): Int =
  cursos.indices.toVector
    .filter(i => a(i) >= 0 && capAula(aulas(a(i))) >= estCurso(cursos(i)))
    .map(i => capAula(aulas(a(i))) - estCurso(cursos(i)))
    .sum
```

**Argumentación:** El `filter` descarta los cursos con fallo (donde el $\max$ sería 0). En el subconjunto restante la diferencia siempre es no negativa, por lo que `map` + `sum` calcula exactamente la especificación. ✓

---

### `movilidad`

**Especificación:**

$$\text{MV}^\alpha_{C,A,D} = \sum_{j=0}^{k-2} D\!\left[\alpha_{\sigma_j},\, \alpha_{\sigma_{j+1}}\right]$$

donde $\sigma$ es la permutación que ordena los cursos asignados por hora de inicio.

**Implementación:**

```scala
def movilidad(cursos: Cursos, aulas: Aulas, d: Distancias,
              a: Asignacion): Int = {
  val ordenados = cursos.indices.toVector
    .filter(i => a(i) >= 0)
    .sortBy(i => iniCurso(cursos(i)))
  if (ordenados.length < 2) 0
  else
    ordenados.zip(ordenados.tail)
      .map { case (i, j) => d(a(i))(a(j)) }
      .sum
}
```

**Argumentación:**
1. `filter` retiene solo los índices asignados.
2. `sortBy iniCurso` produce la permutación $\sigma_0, \ldots, \sigma_{k-1}$.
3. `zip` con `tail` genera los pares $(\sigma_j, \sigma_{j+1})$ para $j = 0, \ldots, k-2$.
4. `map` + `sum` calcula $\sum D[\alpha_{\sigma_j}][\alpha_{\sigma_{j+1}}]$.

Coincide exactamente con la especificación. ✓

---

### `costoAsignacion`

**Especificación:**

$$\text{CT}^\alpha = w_{CH} \cdot \text{CH}^\alpha + w_{CF} \cdot \text{CF}^\alpha + w_{DE} \cdot \text{DE}^\alpha + w_{MV} \cdot \text{MV}^\alpha$$

**Implementación:**

```scala
def costoAsignacion(cursos: Cursos, aulas: Aulas, d: Distancias,
                    a: Asignacion, w: Pesos): Int = {
  val (wCH, wCF, wDE, wMV) = w
  wCH * choques(cursos, a) +
  wCF * capacidadFallida(cursos, aulas, a) +
  wDE * desperdicio(cursos, aulas, a) +
  wMV * movilidad(cursos, aulas, d, a)
}
```

**Argumentación:** La función es una combinación lineal directa de las cuatro funciones anteriores (todas correctas). La corrección se reduce a la aritmética de enteros de Scala. ✓

---

### `generarAsignaciones`

**Especificación:**

$$\text{gen}(n, m) = \left\{ \alpha \in \{0,\ldots,m-1\}^n \right\}$$

**Implementación:**

```scala
def generarAsignaciones(n: Int, m: Int): Vector[Asignacion] =
  if (n == 0) Vector(Vector.empty)
  else
    generarAsignaciones(n - 1, m).flatMap(asig =>
      (0 until m).toVector.map(j => asig :+ j)
    )
```

**Prueba por inducción estructural sobre $n$:**

Sea $P(n)$: `generarAsignaciones(n, m)` devuelve exactamente todos los vectores de longitud $n$ sobre $\{0,\ldots,m-1\}$.

- **Caso base** $n = 0$:

$$P_f(0) \to \texttt{Vector(Vector.empty)}$$

El único elemento de $\{0,\ldots,m-1\}^0$ es el vector vacío. Por tanto $P(0)$ es verdadero. ✓

- **Caso inductivo** $n = k+1$, hipótesis $P(k)$:

$$P_f(k+1) \to \text{gen}(k, m).\text{flatMap}\!\left(\text{asig} \mapsto \{0,\ldots,m{-}1\}.\text{map}(j \mapsto \text{asig} \mathrel{:+} j)\right)$$

Por $P(k)$, `gen(k, m)` contiene todos los vectores de longitud $k$. Para cada uno de ellos se generan $m$ extensiones añadiendo cada $j \in \{0,\ldots,m-1\}$. El resultado es exactamente $\{0,\ldots,m-1\}^{k+1}$. Por tanto $P(k+1)$ es verdadero. ✓

**Conclusión:** $\forall n \geq 0 : P(n)$. ✓

---

### `asignacionOptima`

**Especificación:**

$$\alpha^* = \arg\min_{\alpha \in \{0,\ldots,m-1\}^n} \text{CT}^\alpha$$

**Implementación:**

```scala
def asignacionOptima(cursos: Cursos, aulas: Aulas, d: Distancias,
                     w: Pesos): (Asignacion, Int) =
  generarAsignaciones(cursos.length, aulas.length)
    .map(a => (a, costoAsignacion(cursos, aulas, d, a, w)))
    .minBy(_._2)
```

**Argumentación:** Por la corrección de `generarAsignaciones`, el vector de candidatas contiene **todas** las asignaciones en $\{0,\ldots,m-1\}^n$. Por la corrección de `costoAsignacion`, cada tupla $(a, \text{CT})$ tiene el costo correcto. `minBy(_._2)` selecciona la tupla con menor costo, que es por exhaustividad la asignación óptima global. ✓

---

## Casos de prueba

Los casos de prueba se encuentran en:

- `src/test/scala/proyecto/AsignacionAulasTest.scala` — funciones secuenciales (≥ 5 casos por función).
- `src/test/scala/proyecto/AsignacionAulasParTest.scala` — funciones paralelas (≥ 5 casos por función).

Se ejecutan automáticamente con:

```bash
./gradlew test
```

El reporte se genera en `app/build/reports/tests/test/index.html`.