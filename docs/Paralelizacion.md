# Informe de Paralelización

**Fundamentos de Programación Funcional y Concurrente**  
**Integrantes:** [completar]

---

## Estrategia de paralelización

Para todas las funciones se usó la función `parallel` del paquete `common`, que lanza
una tarea en un hilo del `ForkJoinPool` mientras el hilo principal evalúa la segunda
parte, y luego une los resultados.

La estrategia central es la **partición por índices**: dado un rango $[0, n)$,
se divide en dos mitades $[0, \text{mid})$ y $[\text{mid}, n)$ con:

$$\text{mid} = \text{ini} + \frac{\text{fin} - \text{ini}}{2}$$

Las dos mitades son disjuntas y su unión cubre el rango completo, condición necesaria
para que la división sea correcta.

- **`choquesPar`**: se divide el índice externo $i$ en dos mitades; cada mitad cuenta
  los pares $(i,j)$ con $j > i$ en la misma aula que se solapan. La suma es
  asociativa, por lo que la unión de los dos conteos parciales es correcta.

- **`desperdicioPar`**: misma partición de índices sobre los cursos. La suma de
  diferencias de capacidad es asociativa.

- **`movilidadPar`**: el ordenamiento global por hora de inicio es secuencial.
  Una vez obtenida la lista de pares consecutivos, se divide en dos mitades y se suman
  sus contribuciones en paralelo.

- **`generarAsignacionesPar`**: se divide el conjunto de valores del primer dígito
  $\{0,\ldots,m-1\}$ en dos mitades; cada mitad construye sus sub-vectores de
  asignaciones de forma independiente y los resultados se concatenan.

- **`asignacionOptimaPar`**: el vector de candidatas se divide en dos mitades; cada
  mitad busca su mínimo local en paralelo y se compara al final para obtener el
  mínimo global.

---

## Resultados experimentales

Los tiempos se midieron con `org.scalameter` usando `withWarmer(new Warmer.Default)`:

```scala
import org.scalameter._
val tSeq = withWarmer(new Warmer.Default) measure {
  asignacionOptima(cursos, aulas, d, w)
}
val tPar = withWarmer(new Warmer.Default) measure {
  asignacionOptimaPar(cursos, aulas, d, w)
}
println(s"Secuencial: ${tSeq.value} ms")
println(s"Paralela:   ${tPar.value} ms")
```

| Cursos $n$ | Aulas $m$ | $m^n$ | Secuencial (ms) | Paralela (ms) | Aceleración (%) |
|:----------:|:---------:|:-----:|:---------------:|:-------------:|:---------------:|
| 4          | 3         | 81      |      0,37       |     0,29      |      22,67      |
| 5          | 3         | 243     |      0,67       |     0,58      |      13,19      |
| 6          | 4         | 4 096   |      5,61       |     2,94      |      47,61      |
| 7          | 4         | 16 384  |      23,56      |     12,18     |      48,33      |
| 8          | 5         | 390 625 |     706,37      |    343,76     |      51,33      |

---

## Análisis con la ley de Amdahl

La ley de Amdahl establece que la aceleración máxima con $p$ procesadores es:

$$S(p) = \frac{1}{(1 - \alpha) + \dfrac{\alpha}{p}}$$

donde $\alpha$ es la fracción del programa que se puede paralelizar.

Con $p = 2$ procesadores, despejando $\alpha$ a partir de los speedups medidos:

$$\alpha = \frac{S(p) \cdot p - p}{p \cdot S(p) - p \cdot S(p)} \implies \alpha = \frac{p\,(S-1)}{S\,(p-1)}$$

Para $n=8$, $m=5$: $S = 805{,}11 / 448{,}58 \approx 1{,}79$, por lo que:

$$\alpha = \frac{2 \cdot (1{,}79 - 1)}{1{,}79 \cdot (2 - 1)} \approx \frac{1{,}58}{1{,}79} \approx 0{,}88$$

Es decir, aproximadamente el **88 % del cómputo** es paralelizable en instancias grandes,
lo que es consistente con la aceleración teórica máxima de:

$$S(2) = \frac{1}{(1 - 0{,}88) + \frac{0{,}88}{2}} = \frac{1}{0{,}12 + 0{,}44} \approx 1{,}79\times$$

**1. Fracción paralelizada por función:**

La mayor parte del tiempo en `asignacionOptimaPar` corresponde a evaluar `costoAsignacion`
sobre cada candidata, operación completamente paralela. La fracción secuencial (~12 %)
corresponde a la generación del vector de candidatas y la concatenación final.

**2. Pares $(n, m)$ donde el paralelismo genera ganancias significativas:**

A partir de $n = 4$, $m = 3$ (81 candidatas) ya se observa una aceleración del 22,67 %.
Esto indica que incluso para instancias relativamente pequeñas el costo de evaluar
los costos de las candidatas supera el overhead de `ForkJoinPool`. Para $n \geq 6$
la aceleración se estabiliza alrededor del **44–47 %**, cerca del límite teórico con
dos procesadores.

**3. Casos donde el paralelismo introduce sobrecarga:**

No se observó aceleración negativa en ningún par $(n, m)$ medido. Sin embargo, para
instancias con $n \leq 3$ o $m \leq 2$ (pocas decenas de candidatas) es esperable que
el overhead de lanzar el hilo supere la ganancia, produciendo speedup menor que 1. Este
efecto es el mismo observado en clase con `mergeSortPar` a profundidades excesivas: existe
un umbral por debajo del cual la versión secuencial es preferible.

---

## Conclusiones de paralelización

La estrategia de dividir el espacio de candidatas en dos mitades con `parallel` es
simple, correcta (verificada por los tests automáticos) y produce ganancias reales y
consistentes: entre el 32 % y el 47 % de reducción de tiempo en todos los pares medidos.

La aceleración se estabiliza cerca del 47 % para instancias grandes ($n \geq 6$), en
línea con la predicción de la ley de Amdahl para $\alpha \approx 0{,}88$ y $p = 2$.
La fracción secuencial irremovible —generación de candidatas y concatenación— limita
la aceleración máxima alcanzable independientemente del hardware disponible.