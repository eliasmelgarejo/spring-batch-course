PLAN DE ESTUDIOS: SPRING BATCH PARA DESARROLLADORES JUNIOR
==================================================

OBJETIVO GENERAL
---------------
Al finalizar el curso, el desarrollador será capaz de diseñar e implementar soluciones de procesamiento batch empresarial utilizando Spring Batch, con énfasis especial en la automatización de procesos contables y manejo eficiente de grandes volúmenes de datos.

OBJETIVOS ESPECÍFICOS
-------------------
- Comprender la arquitectura y componentes fundamentales de Spring Batch
- Dominar la lectura y escritura de datos desde diversas fuentes, especialmente PostgreSQL
- Implementar lógica de procesamiento compleja para transformaciones de datos empresariales
- Desarrollar un sistema completo de generación de asientos contables automatizados
- Aplicar mejores prácticas en el manejo de errores y control de transacciones

ESTRUCTURA DEL CURSO
------------------
Total: 20 lecciones
Duración por lección: 2 horas (1 hora teoría + 1 hora práctica)
Duración total: 40 horas
Evaluación continua: 30%
Proyecto final: 70%

MÓDULO 1: FUNDAMENTOS DE SPRING BATCH
------------------------------------

Lección 1: Introducción a Spring Batch
- Conceptos fundamentales del procesamiento batch
- Arquitectura de Spring Batch
- Componentes principales: Jobs, Steps, Chunks
- Práctica: Configuración del ambiente y primer Job

Lección 2: Anatomía de un Job
- Estructura detallada de Jobs y Steps
- JobParameters y su importancia
- JobRepository y MetaData
- Práctica: Implementación de Job multi-step con parámetros

Lección 3: Configuración y Contexto
- Configuración basada en Java vs XML
- Spring Boot Integration
- Scopes en Spring Batch
- Práctica: Migración de configuración XML a Java

MÓDULO 2: LECTURA Y ESCRITURA DE DATOS
-------------------------------------

Lección 4: Fundamentos de ItemReader
- Tipos de ItemReader
- Configuración de PostgreSQL
- Paginación y cursores
- Práctica: Implementación de JdbcPagingItemReader

Lección 5: ItemReader Avanzado
- Lectura de múltiples fuentes
- Lecturas condicionadas
- Manejo de estados
- Práctica: Reader compuesto para ventas y compras

Lección 6: ItemWriter Básico
- Conceptos de ItemWriter
- Escritura en bases de datos
- Transaccionalidad
- Práctica: Implementación de JdbcBatchItemWriter

Lección 7: ItemWriter Avanzado
- Escritura en múltiples destinos
- Clasificación de salidas
- Composición de writers
- Práctica: Writer para asientos contables

MÓDULO 3: PROCESAMIENTO DE DATOS
-------------------------------

Lección 8: ItemProcessor Fundamentals
- Rol del ItemProcessor
- Filtrado y validación
- Transformación de datos
- Práctica: Processor para cálculos contables básicos

Lección 9: Procesamiento Avanzado
- Cadena de processors
- Procesamiento condicional
- Validación de negocio
- Práctica: Procesamiento de reglas contables complejas

Lección 10: Control de Flujo
- Decisiones basadas en resultados
- Steps condicionales
- Flujos paralelos
- Práctica: Implementación de flujos de decisión contable

MÓDULO 4: MANEJO DE ERRORES Y REINTENTOS
---------------------------------------

Lección 11: Gestión de Errores
- Skip y retry policies
- Listeners de error
- Logging y trazabilidad
- Práctica: Implementación de política de reintentos

Lección 12: Transacciones
- Manejo transaccional en Spring Batch
- Puntos de commit
- Rollback y recuperación
- Práctica: Gestión transaccional en asientos contables

MÓDULO 5: OPTIMIZACIÓN Y ESCALABILIDAD
------------------------------------

Lección 13: Particionamiento
- Procesamiento paralelo
- Estrategias de particionamiento
- Configuración de hilos
- Práctica: Particionamiento de procesamiento contable

Lección 14: Escalabilidad
- Chunk vs Tasklet
- Tamaño óptimo de chunk
- Monitoreo de rendimiento
- Práctica: Optimización de rendimiento

MÓDULO 6: PROYECTO INTEGRADOR
----------------------------

Lección 15: Diseño del Sistema Contable
- Análisis de requerimientos
- Diseño de la solución
- Modelado de datos
- Práctica: Diseño de la arquitectura

Lección 16: Implementación de Lecturas
- Configuración de fuentes de datos
- Implementación de readers personalizados
- Validación de datos de entrada
- Práctica: Implementación de lectores

Lección 17: Procesamiento Contable
- Reglas de negocio contable
- Transformación de datos
- Validaciones específicas
- Práctica: Implementación de procesadores

Lección 18: Generación de Asientos
- Escritura de asientos contables
- Validación de balances
- Control de errores específicos
- Práctica: Implementación de writers

Lección 19: Testing y Calidad
- Pruebas unitarias
- Pruebas de integración
- Cobertura de código
- Práctica: Implementación de suite de pruebas

Lección 20: Despliegue y Monitoreo
- Configuración de producción
- Monitoreo y alertas
- Mantenimiento
- Práctica: Configuración de ambiente productivo

SISTEMA DE EVALUACIÓN
--------------------

Evaluación Continua (30%):
- Ejercicios prácticos por lección: 15%
- Quizzes teóricos: 5%
- Participación y entregas parciales: 10%

Proyecto Final (70%):
- Implementación del sistema: 40%
- Documentación: 15%
- Presentación y defensa: 15%

CRITERIOS DE APROBACIÓN
----------------------
- Asistencia mínima: 80%
- Nota mínima en evaluación continua: 60%
- Nota mínima en proyecto final: 70%
- Nota global mínima: 70%

PROYECTO TRANSVERSAL
-------------------
A lo largo del curso se desarrollará un sistema de generación automática de asientos contables que incluirá:
- Lectura de transacciones (ventas y compras) desde PostgreSQL
- Aplicación de reglas contables configurables
- Generación y validación de asientos contables
- Control de errores y excepciones
- Reportes de procesamiento
- Interfaz de monitoreo
