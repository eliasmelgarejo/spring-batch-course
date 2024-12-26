MÓDULO 1: FUNDAMENTOS DE SPRING BATCH
====================================

LECCIÓN 1: INTRODUCCIÓN A SPRING BATCH
------------------------------------

CONTENIDO TEÓRICO (1 hora)

1. ¿Qué es el procesamiento batch?
   El procesamiento batch es un método de computación donde un conjunto de transacciones se acumula durante un tiempo y se procesa todo junto, en lugar de procesarse individualmente. En el contexto empresarial, estos procesos suelen ejecutarse en momentos de baja carga del sistema, como durante la noche o en fines de semana.

2. Arquitectura de Spring Batch
   Spring Batch se construye sobre una arquitectura en capas:

- Capa de Aplicación: Contiene el código específico de nuestros jobs y la lógica de negocio.
- Capa Core: Contiene las funcionalidades core para lanzar y controlar jobs.
- Capa de Infraestructura: Proporciona componentes comunes utilizados tanto por la aplicación como por el core.

3. Componentes Principales:
- Job: Representa un proceso batch completo
- Step: Una fase independiente dentro de un Job
- ItemReader: Lee los datos de una fuente
- ItemProcessor: Procesa los datos
- ItemWriter: Escribe los datos procesados

PRÁCTICA (1 hora)

Configuración del Ambiente:

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-batch</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
</dependencies>
```

Configuración básica:

```java
package com.springbatchcourse.infrastructure.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
   private final JobRepository jobRepository;
   private final PlatformTransactionManager transactionManager;

   public BatchConfiguration(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
      this.jobRepository = jobRepository;
      this.transactionManager = transactionManager;
   }

   @Bean
   public Job helloWorldJob() {
      return new JobBuilder("helloWorldJob", jobRepository)
              .start(helloWorldStep())
              .build();
   }

   @Bean
   public Step helloWorldStep() {
      return new StepBuilder("helloWorldStep", jobRepository)
              .<String, String>chunk(10, transactionManager)
              .reader(() -> {
                 System.out.println("¡Hola Mundo desde Spring Batch!");
                 return String.valueOf(RepeatStatus.FINISHED);
              })
              .build();
   }
}

```

EJERCICIO PRÁCTICO:
Crear un Job que lea un parámetro con el nombre del usuario y muestre un saludo personalizado.

EVALUACIÓN:
Quiz sobre conceptos básicos (20 minutos)

LECCIÓN 2: ANATOMÍA DE UN JOB
----------------------------

CONTENIDO TEÓRICO (1 hora)

1. Estructura de un Job
   Un Job es una secuencia ordenada de Steps que define un proceso batch. Características principales:
- Puede ser configurado para ejecutarse con o sin parámetros
- Mantiene estado durante la ejecución
- Puede ser reiniciado en caso de fallo

2. Steps y su Configuración
   Tipos de Steps:
- Tasklet: Para operaciones simples
- Chunk-oriented: Para procesamiento de datos en lotes

3. JobParameters
   Permiten parametrizar la ejecución de un Job:
- Tipos de parámetros: String, Long, Double, Date
- Identificación única de ejecuciones
- Reutilización de parámetros

PRÁCTICA (1 hora)

Implementación de un Job con múltiples Steps:

```java
@Configuration
public class MultiStepJobConfig {
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job multiStepJob() {
        return jobBuilderFactory.get("multiStepJob")
                .start(step1())
                .next(step2())
                .next(step3())
                .build();
    }
    
    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
                .tasklet((contribution, chunkContext) -> {
                    // Obtener parámetros
                    JobParameters parameters = 
                        chunkContext.getStepContext().getStepExecution()
                                  .getJobParameters();
                    String fecha = parameters.getString("fecha");
                    System.out.println("Paso 1: Iniciando proceso para fecha " + fecha);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    // Similar implementación para step2 y step3...
}
```
LECCIÓN 3: CONFIGURACIÓN Y CONTEXTO
-------------------------------------------

CONTENIDO TEÓRICO (1 hora)

1. Configuración basada en Java vs XML

La configuración en Spring Batch puede realizarse de dos formas principales. Históricamente, Spring utilizaba XML, pero la tendencia moderna favorece la configuración basada en Java. Veamos las diferencias y ventajas de cada aproximación:

Configuración Java:
```java
@Configuration
@EnableBatchProcessing
public class JobConfig {
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Bean
    public Job processingJob(Step processingStep) {
        return jobBuilderFactory.get("processingJob")
                .incrementer(new RunIdIncrementer())
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        // Lógica previa al job
                    }
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        // Lógica posterior al job
                    }
                })
                .flow(processingStep)
                .end()
                .build();
    }
}
```

Equivalente en XML:
```xml
<job id="processingJob" xmlns="http://www.springframework.org/schema/batch">
    <step id="processingStep">
        <tasklet>
            <chunk reader="itemReader" 
                   processor="itemProcessor"
                   writer="itemWriter" 
                   commit-interval="10"/>
        </tasklet>
    </step>
</job>
```

2. Spring Boot Integration

Spring Boot simplifica significativamente la configuración de Spring Batch. Veamos cómo se integran:

```java
@SpringBootApplication
@EnableBatchProcessing
public class BatchApplication {
    public static void main(String[] args) {
        // La configuración de la base de datos de metadatos se realiza automáticamente
        SpringApplication.run(BatchApplication.class, args);
    }
    
    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }
}
```

3. Scopes en Spring Batch

Spring Batch introduce nuevos scopes específicos que son cruciales para el manejo correcto de los jobs:

```java
@Configuration
public class ScopedBeanConfig {
    
    // Bean a nivel de Job
    @Bean
    @JobScope
    public ItemReader<Transaction> reader(@Value("#{jobParameters['fecha']}") String fecha) {
        // Este bean se crea una vez por cada ejecución del job
        return new CustomReader(fecha);
    }
    
    // Bean a nivel de Step
    @Bean
    @StepScope
    public ItemProcessor<Transaction, AccountingEntry> processor(
            @Value("#{stepExecutionContext['processingDate']}") LocalDate processingDate) {
        // Este bean se crea una vez por cada ejecución del step
        return new CustomProcessor(processingDate);
    }
}
```

PRÁCTICA (1 hora)

Implementaremos un job que demuestre el uso de diferentes scopes y la integración con Spring Boot:

```java
@Configuration
public class AccountingJobConfig {
    
    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job accountingJob() {
        return jobBuilderFactory.get("accountingJob")
                .start(importStep())
                .next(processStep())
                .next(validateStep())
                .build();
    }
    
    @Bean
    @JobScope
    public Step importStep() {
        return stepBuilderFactory.get("importStep")
                .<Transaction, Transaction>chunk(10)
                .reader(transactionReader(null))  // El parámetro se inyectará en runtime
                .writer(transactionWriter())
                .build();
    }
    
    @Bean
    @StepScope
    public ItemReader<Transaction> transactionReader(
            @Value("#{jobParameters['fecha']}") String fecha) {
        CustomReader reader = new CustomReader();
        reader.setFecha(fecha);
        return reader;
    }
    
    // Las clases necesarias para el ejemplo:
    
    public class Transaction {
        private LocalDate fecha;
        private BigDecimal monto;
        private String tipo;
        // getters y setters
    }
    
    public class CustomReader implements ItemReader<Transaction> {
        private String fecha;
        
        @Override
        public Transaction read() {
            // Lógica de lectura
            return null;
        }
        
        public void setFecha(String fecha) {
            this.fecha = fecha;
        }
    }
}
```

EJERCICIO PRÁCTICO:
Crear una configuración completa que incluya:
1. Job con tres steps
2. Uso de JobScope y StepScope
3. Lectura de parámetros del job
4. Implementación de listeners para logging

EVALUACIÓN (30 minutos):
1. Implementar un job que procese datos con diferentes configuraciones de scope
2. Explicar las diferencias entre JobScope y StepScope
3. Convertir una configuración XML dada a configuración Java

