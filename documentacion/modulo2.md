Continuemos con el Módulo 2, que se centra en la lectura y escritura de datos, lo cual será fundamental para nuestro proyecto contable.

MÓDULO 2: LECTURA Y ESCRITURA DE DATOS
====================================

LECCIÓN 4: FUNDAMENTOS DE ITEMREADER
----------------------------------

CONTENIDO TEÓRICO (1 hora)

1. Introducción a ItemReader
---------------------------

Los ItemReaders son componentes fundamentales en Spring Batch que nos permiten leer datos de diversas fuentes de manera eficiente y controlada. En nuestro caso de uso contable, esto es crucial para leer transacciones de manera confiable desde PostgreSQL.

Un ItemReader debe implementar una interfaz simple pero poderosa:

```java
public interface ItemReader<T> {
    T read() throws Exception;
}
```

El método read() tiene tres posibles comportamientos:
- Retorna un item cuando encuentra uno
- Retorna null cuando llega al final de los datos
- Lanza una excepción si ocurre un error durante la lectura

2. Configuración de PostgreSQL para Spring Batch
----------------------------------------------

Primero, configuremos la conexión a PostgreSQL de manera adecuada para procesamiento batch:

```java
@Configuration
public class DatabaseConfig {
    @Bean
    public DataSource batchDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/contabilidad");
        dataSource.setUsername("batch_user");
        dataSource.setPassword("batch_password");
        
        // Configuraciones importantes para procesamiento batch
        dataSource.setMaximumPoolSize(10);  // Límite de conexiones concurrentes
        dataSource.setMinimumIdle(5);       // Conexiones mínimas en el pool
        dataSource.setIdleTimeout(300000);  // Timeout de conexiones inactivas
        
        return dataSource;
    }
    
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(batchDataSource());
    }
}
```

3. JdbcCursorItemReader vs JdbcPagingItemReader
---------------------------------------------

Existen dos aproximaciones principales para leer datos de bases de datos en Spring Batch:

JdbcCursorItemReader:
- Mantiene un cursor abierto durante toda la lectura
- Eficiente en memoria
- Requiere mantener la conexión abierta
- Ideal para conjuntos de datos pequeños o medianos

```java
@Bean
public JdbcCursorItemReader<Transaccion> cursorReader() {
    return new JdbcCursorItemReaderBuilder<Transaccion>()
        .name("transaccionReader")
        .dataSource(dataSource)
        .sql("SELECT * FROM transacciones WHERE fecha = ? ORDER BY id")
        .rowMapper(new TransaccionRowMapper())
        .preparedStatementSetter(
            new SingleItemPreparedStatementSetter(new Date())
        )
        .build();
}
```

JdbcPagingItemReader:
- Lee los datos en páginas
- Permite múltiples hilos
- Mejor para grandes volúmenes de datos
- Más resistente a problemas de conexión

```java
@Bean
public JdbcPagingItemReader<Transaccion> pagingReader() {
    // Definimos la consulta paginada
    PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
    queryProvider.setSelectClause("SELECT id, fecha, monto, tipo");
    queryProvider.setFromClause("FROM transacciones");
    queryProvider.setWhereClause("fecha = :fecha");
    queryProvider.setSortKeys(Collections.singletonMap("id", Order.ASCENDING));

    return new JdbcPagingItemReaderBuilder<Transaccion>()
        .name("pagingTransaccionReader")
        .dataSource(dataSource)
        .pageSize(1000)
        .queryProvider(queryProvider)
        .parameterValues(Collections.singletonMap("fecha", new Date()))
        .rowMapper(new TransaccionRowMapper())
        .build();
}
```

PRÁCTICA (1 hora)

Implementaremos un lector robusto para nuestro sistema contable:

```java
@Component
public class TransaccionReader {
    
    @Autowired
    private DataSource dataSource;
    
    @Bean
    @StepScope
    public ItemReader<Transaccion> transaccionReader(
            @Value("#{jobParameters['fecha']}") String fecha,
            @Value("#{jobParameters['tipoTransaccion']}") String tipo) {
        
        PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
        
        // Construimos una consulta dinámica según el tipo de transacción
        StringBuilder selectClause = new StringBuilder(
            "SELECT t.id, t.fecha, t.monto, t.tipo_comprobante, ");
            
        if ("VENTA".equals(tipo)) {
            selectClause.append("c.nombre as cliente, t.condicion_pago ");
            queryProvider.setFromClause("FROM ventas t JOIN clientes c ON t.cliente_id = c.id");
        } else {
            selectClause.append("p.nombre as proveedor, t.condicion_pago ");
            queryProvider.setFromClause("FROM compras t JOIN proveedores p ON t.proveedor_id = p.id");
        }
        
        queryProvider.setSelectClause(selectClause.toString());
        queryProvider.setWhereClause("t.fecha = :fecha AND t.procesado = false");
        queryProvider.setSortKeys(Collections.singletonMap("t.id", Order.ASCENDING));

        // Configuramos los parámetros
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("fecha", java.sql.Date.valueOf(fecha));

        // Creamos el reader con configuración optimizada
        return new JdbcPagingItemReaderBuilder<Transaccion>()
            .name("transaccionReader")
            .dataSource(dataSource)
            .pageSize(500)  // Tamaño óptimo para nuestro caso de uso
            .queryProvider(queryProvider)
            .parameterValues(parametros)
            .rowMapper(new TransaccionRowMapper())
            .saveState(true)  // Importante para reinicio del job
            .build();
    }
}

// Implementamos un RowMapper personalizado
class TransaccionRowMapper implements RowMapper<Transaccion> {
    @Override
    public Transaccion mapRow(ResultSet rs, int rowNum) throws SQLException {
        Transaccion transaccion = new Transaccion();
        transaccion.setId(rs.getLong("id"));
        transaccion.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        transaccion.setMonto(rs.getBigDecimal("monto"));
        transaccion.setTipoComprobante(rs.getString("tipo_comprobante"));
        transaccion.setCondicionPago(rs.getString("condicion_pago"));
        
        // Mapeamos campos específicos según el tipo
        if (rs.getMetaData().getColumnCount() > 5) {
            if (rs.findColumn("cliente") > 0) {
                transaccion.setCliente(rs.getString("cliente"));
            } else {
                transaccion.setProveedor(rs.getString("proveedor"));
            }
        }
        
        return transaccion;
    }
}
```

EJERCICIO PRÁCTICO:
Implementar un reader que:
1. Lea transacciones pendientes de procesar
2. Maneje errores de conexión apropiadamente
3. Implemente logging detallado
4. Sea capaz de reiniciar desde el último punto procesado

En la siguiente lección, veremos cómo manejar lecturas más complejas y cómo integrar múltiples fuentes de datos.

Continuemos con la Lección 5, que se centra en aspectos más avanzados de la lectura de datos, lo cual será especialmente útil para nuestro sistema contable.

LECCIÓN 5: ITEMREADER AVANZADO
-----------------------------

CONTENIDO TEÓRICO (1 hora)

1. Lectura de Múltiples Fuentes
------------------------------

En un sistema contable real, frecuentemente necesitamos procesar datos de diferentes fuentes. Por ejemplo, podríamos necesitar combinar información de ventas, compras y ajustes manuales. Spring Batch nos proporciona herramientas poderosas para manejar estos escenarios complejos.

Veamos cómo implementar un reader compuesto que maneje múltiples fuentes:

```java
@Configuration
public class MultipleSourceReaderConfig {

    @Bean
    @StepScope
    public CompositeItemReader<Transaccion> compositeTransactionReader(
            @Value("#{jobParameters['fecha']}") String fecha) {
        
        CompositeItemReader<Transaccion> compositeReader = new CompositeItemReader<>();
        
        // Creamos una lista de readers individuales
        List<ItemReader<Transaccion>> readers = new ArrayList<>();
        
        // Añadimos los readers específicos
        readers.add(ventasReader(fecha));
        readers.add(comprasReader(fecha));
        readers.add(ajustesReader(fecha));
        
        compositeReader.setDelegates(readers);
        
        return compositeReader;
    }

    private ItemReader<Transaccion> ventasReader(String fecha) {
        // Implementamos un reader específico para ventas que incluye
        // lógica de manejo de estados y validación
        
        JdbcPagingItemReader<Transaccion> reader = new JdbcPagingItemReaderBuilder<Transaccion>()
            .name("ventasReader")
            .dataSource(dataSource)
            .queryProvider(createVentasQueryProvider())
            .parameterValues(Collections.singletonMap("fecha", fecha))
            .rowMapper(new VentasRowMapper())
            .pageSize(500)
            .build();
            
        // Añadimos un wrapper para manejar el estado
        return new StateHandlingItemReaderWrapper<>(reader);
    }
}
```

2. Lecturas Condicionadas
------------------------

En ocasiones necesitamos aplicar lógica condicional durante la lectura. Por ejemplo, podríamos querer filtrar ciertas transacciones basándonos en reglas de negocio:

```java
@Component
public class ConditionalTransactionReader implements ItemReader<Transaccion> {
    
    private final ItemReader<Transaccion> delegate;
    private final TransaccionValidator validator;
    
    @Autowired
    public ConditionalTransactionReader(
            ItemReader<Transaccion> delegate,
            TransaccionValidator validator) {
        this.delegate = delegate;
        this.validator = validator;
    }
    
    @Override
    public Transaccion read() throws Exception {
        Transaccion transaccion;
        
        while ((transaccion = delegate.read()) != null) {
            // Aplicamos reglas de negocio
            if (validator.esTransaccionValida(transaccion)) {
                enriquecerTransaccion(transaccion);
                return transaccion;
            }
            // Si la transacción no es válida, continuamos con la siguiente
        }
        
        return null;
    }
    
    private void enriquecerTransaccion(Transaccion transaccion) {
        // Añadimos información adicional necesaria
        // Por ejemplo, tipos de cambio o información contable
    }
}
```

3. Manejo de Estados
-------------------

El manejo correcto del estado es crucial para permitir la recuperación en caso de fallos:

```java
@Component
public class StateHandlingItemReaderWrapper<T> implements ItemStreamReader<T> {
    
    private final ItemReader<T> delegate;
    private ExecutionContext executionContext;
    private int currentItemCount;
    private static final String CURRENT_COUNT = "current.count";
    
    public StateHandlingItemReaderWrapper(ItemReader<T> delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public T read() throws Exception {
        currentItemCount++;
        T item = delegate.read();
        
        if (item != null) {
            // Guardamos el estado actual
            executionContext.putInt(CURRENT_COUNT, currentItemCount);
        }
        
        return item;
    }
    
    @Override
    public void open(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        
        if (executionContext.containsKey(CURRENT_COUNT)) {
            // Recuperamos el estado anterior
            currentItemCount = executionContext.getInt(CURRENT_COUNT);
            
            // Avanzamos hasta el último punto procesado
            for (int i = 0; i < currentItemCount; i++) {
                try {
                    delegate.read();
                } catch (Exception e) {
                    throw new ItemStreamException(
                        "Error recuperando estado del reader", e);
                }
            }
        }
    }
}
```

PRÁCTICA (1 hora)

Implementemos un sistema completo de lectura para nuestro proyecto contable:

```java
@Configuration
public class ContabilidadReaderConfig {

    @Bean
    @StepScope
    public ItemReader<DocumentoContable> documentoContableReader(
            @Value("#{jobParameters['fecha']}") String fecha,
            @Value("#{jobParameters['tiposProceso']}") String tiposProceso) {
            
        // Creamos un reader compuesto que maneje múltiples tipos de documentos
        CompositeItemReader<DocumentoContable> reader = new CompositeItemReader<>();
        List<ItemReader<DocumentoContable>> readers = new ArrayList<>();
        
        // Parseamos los tipos de proceso solicitados
        List<String> tipos = Arrays.asList(tiposProceso.split(","));
        
        if (tipos.contains("VENTAS")) {
            readers.add(new StateHandlingItemReaderWrapper<>(
                createVentasReader(fecha)));
        }
        
        if (tipos.contains("COMPRAS")) {
            readers.add(new StateHandlingItemReaderWrapper<>(
                createComprasReader(fecha)));
        }
        
        if (tipos.contains("AJUSTES")) {
            readers.add(new StateHandlingItemReaderWrapper<>(
                createAjustesReader(fecha)));
        }
        
        reader.setDelegates(readers);
        
        // Añadimos validación y enriquecimiento
        return new ConditionalDocumentReader(
            reader,
            documentoValidator,
            contextService
        );
    }
}

@Component
public class ConditionalDocumentReader implements ItemStreamReader<DocumentoContable> {
    
    private final ItemReader<DocumentoContable> delegate;
    private final DocumentoValidator validator;
    private final ContextService contextService;
    
    @Override
    public DocumentoContable read() throws Exception {
        DocumentoContable documento;
        
        while ((documento = delegate.read()) != null) {
            if (validator.esDocumentoValido(documento)) {
                // Enriquecemos el documento con información contextual
                contextService.enriquecerDocumento(documento);
                
                // Registramos la lectura para auditoría
                registrarLectura(documento);
                
                return documento;
            }
            // Si el documento no es válido, lo registramos y continuamos
            registrarDocumentoInvalido(documento);
        }
        
        return null;
    }
    
    private void registrarLectura(DocumentoContable documento) {
        // Implementación del registro de auditoría
    }
}
```

EJERCICIO PRÁCTICO:
Desarrollar un sistema de lectura que:

1. Lea documentos de múltiples fuentes (ventas, compras, ajustes)
2. Implemente validación de documentos según reglas de negocio
3. Maneje el estado para permitir recuperación
4. Incluya registro de auditoría
5. Aplique enriquecimiento de datos

En la siguiente lección, veremos cómo escribir datos procesados en nuestro sistema contable.

LECCIÓN 6: ITEMWRITER BÁSICO
-----------------------------------

Ahora que hemos aprendido sobre la lectura eficiente de datos, pasaremos a escribir los resultados procesados. En nuestro sistema contable, esto significa guardar los asientos contables generados de manera segura y transaccional.

CONTENIDO TEÓRICO (1 hora)

1. Fundamentos de ItemWriter
---------------------------

El ItemWriter es la interfaz fundamental para escribir datos en Spring Batch. Su estructura es simple pero poderosa:

```java
public interface ItemWriter<T> {
    void write(List<? extends T> items) throws Exception;
}
```

A diferencia del ItemReader que lee elementos uno a uno, el ItemWriter recibe una lista de elementos, específicamente un chunk. Esto permite optimizaciones como escrituras en lote.

2. Implementación de un Writer Básico
-----------------------------------

Veamos cómo implementar un writer para nuestros asientos contables usando Spring Data JPA:

```java
@Component
public class AsientoContableItemWriter implements ItemWriter<AsientoContable> {
    
    private final Logger logger = LoggerFactory.getLogger(AsientoContableItemWriter.class);
    private final AsientoContableRepository asientoRepository;
    private final TransaccionRepository transaccionRepository;
    
    @Autowired
    public AsientoContableItemWriter(
            AsientoContableRepository asientoRepository,
            TransaccionRepository transaccionRepository) {
        this.asientoRepository = asientoRepository;
        this.transaccionRepository = transaccionRepository;
    }
    
    @Override
    @Transactional
    public void write(List<? extends AsientoContable> asientos) throws Exception {
        logger.info("Iniciando escritura de {} asientos contables", asientos.size());
        
        for (AsientoContable asiento : asientos) {
            // Validamos el balance antes de guardar
            validarBalance(asiento);
            
            // Guardamos el asiento
            AsientoContable asientoGuardado = asientoRepository.save(asiento);
            
            // Actualizamos el estado de la transacción original
            actualizarTransaccionOrigen(asiento);
            
            logger.debug("Asiento {} guardado exitosamente", asientoGuardado.getId());
        }
        
        logger.info("Escritura de asientos completada exitosamente");
    }
    
    private void validarBalance(AsientoContable asiento) {
        BigDecimal totalDebe = asiento.getLineas().stream()
            .map(LineaAsiento::getDebe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal totalHaber = asiento.getLineas().stream()
            .map(LineaAsiento::getHaber)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new AsientoDesbalanceadoException(
                "El asiento no está balanceado: Debe=" + totalDebe + 
                ", Haber=" + totalHaber);
        }
    }
    
    @Transactional
    private void actualizarTransaccionOrigen(AsientoContable asiento) {
        Optional<Transaccion> transaccionOpt = transaccionRepository
            .findById(asiento.getIdTransaccionOrigen());
            
        transaccionOpt.ifPresent(transaccion -> {
            transaccion.setProcesado(true);
            transaccion.setFechaProcesamiento(LocalDateTime.now());
            transaccionRepository.save(transaccion);
            logger.debug("Transacción {} marcada como procesada", transaccion.getId());
        });
    }
}
```

3. Configuración del Step con el Writer
-------------------------------------

Veamos cómo integrar nuestro writer en un step completo:

```java
@Configuration
public class ContabilidadBatchConfig {
    
    @Bean
    public Step procesarAsientosStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Transaccion> transaccionReader,
            ItemProcessor<Transaccion, AsientoContable> asientoProcessor,
            ItemWriter<AsientoContable> asientoWriter) {
        
        return new StepBuilder("procesarAsientosStep", jobRepository)
            .<Transaccion, AsientoContable>chunk(100, transactionManager)
            .reader(transaccionReader)
            .processor(asientoProcessor)
            .writer(asientoWriter)
            .faultTolerant()
            .retry(OptimisticLockingFailureException.class)
            .retryLimit(3)
            .listener(new StepExecutionListener() {
                @Override
                public void beforeStep(StepExecution stepExecution) {
                    // Logging de inicio del step
                }
                
                @Override
                public ExitStatus afterStep(StepExecution stepExecution) {
                    // Validaciones post-step y logging
                    return ExitStatus.COMPLETED;
                }
            })
            .build();
    }
}
```

PRÁCTICA (1 hora)

Implementemos un writer más completo que incluya validaciones de negocio y manejo de errores:

```java
@Component
public class RobustAsientoContableWriter implements ItemWriter<AsientoContable> {
    
    private final AsientoContableRepository asientoRepository;
    private final TransaccionRepository transaccionRepository;
    private final ValidadorAsientos validadorAsientos;
    private final NotificacionService notificacionService;
    
    // Constructor con inyección de dependencias...
    
    @Override
    @Transactional
    public void write(List<? extends AsientoContable> asientos) throws Exception {
        List<AsientoContable> asientosValidados = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        
        // Primera fase: Validación
        for (AsientoContable asiento : asientos) {
            try {
                validadorAsientos.validarAsiento(asiento);
                asientosValidados.add(asiento);
            } catch (ValidacionException e) {
                errores.add("Error en asiento: " + e.getMessage());
                notificacionService.notificarError(asiento, e);
            }
        }
        
        // Si hay errores, decidimos si continuar o no
        if (!errores.isEmpty() && errores.size() > asientos.size() * 0.1) {
            throw new BatchException(
                "Demasiados errores en el lote: " + errores.size() + 
                " de " + asientos.size() + " asientos");
        }
        
        // Segunda fase: Persistencia
        for (AsientoContable asiento : asientosValidados) {
            try {
                persistirAsientoCompleto(asiento);
            } catch (Exception e) {
                logger.error("Error guardando asiento: {}", asiento.getId(), e);
                throw new ItemWriteException(
                    "Error en persistencia de asiento", e);
            }
        }
    }
    
    @Transactional
    private void persistirAsientoCompleto(AsientoContable asiento) {
        // Guardamos el asiento
        AsientoContable asientoGuardado = asientoRepository.save(asiento);
        
        // Actualizamos la transacción origen
        actualizarTransaccionOrigen(asiento);
        
        // Generamos el registro de auditoría
        generarRegistroAuditoria(asientoGuardado);
    }
}
```

Este writer robusto ofrece:
- Validación completa antes de la persistencia
- Manejo transaccional adecuado
- Notificación de errores
- Registro de auditoría
- Tolerancia configurable a errores


LECCIÓN 7: ITEMWRITER AVANZADO
------------------------------------

En esta lección, exploraremos técnicas avanzadas de escritura que son especialmente relevantes para sistemas contables complejos.

1. ESCRITURA EN MÚLTIPLES DESTINOS
---------------------------------

En sistemas contables reales, frecuentemente necesitamos escribir en varios lugares. Por ejemplo, además de guardar los asientos contables, podríamos necesitar:
- Actualizar saldos de cuentas
- Generar registros de auditoría
- Crear reportes en tiempo real
- Notificar a sistemas externos

Veamos cómo implementar esto de manera elegante:

```java
@Component
public class CompositeAccountingWriter implements ItemWriter<AsientoContable> {
    private final List<ItemWriter<AsientoContable>> delegates;
    private final TransactionTemplate transactionTemplate;
    
    public CompositeAccountingWriter(
            AsientoContableRepository asientoRepository,
            SaldosCuentaRepository saldosRepository,
            AuditoriaService auditoriaService,
            NotificacionService notificacionService,
            PlatformTransactionManager transactionManager) {
            
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        
        // Configuramos los writers delegados
        this.delegates = Arrays.asList(
            new AsientoContableWriter(asientoRepository),
            new SaldosWriter(saldosRepository),
            new AuditoriaWriter(auditoriaService),
            new NotificacionWriter(notificacionService)
        );
    }
    
    @Override
    public void write(List<? extends AsientoContable> asientos) throws Exception {
        // Ejecutamos todo en una única transacción
        transactionTemplate.execute(status -> {
            try {
                // Escribimos en cada destino
                for (ItemWriter<AsientoContable> writer : delegates) {
                    writer.write(asientos);
                }
                return null;
            } catch (Exception e) {
                throw new TransactionSystemException(
                    "Error en escritura múltiple", e);
            }
        });
    }
}
```

2. CLASIFICACIÓN DE SALIDAS
--------------------------

A veces necesitamos dirigir diferentes asientos a diferentes destinos según su naturaleza:

```java
@Component
public class ClassifiedAccountingWriter implements ItemWriter<AsientoContable> {
    
    private final Map<TipoAsiento, ItemWriter<AsientoContable>> writersPorTipo;
    
    public ClassifiedAccountingWriter(
            ItemWriter<AsientoContable> ventasWriter,
            ItemWriter<AsientoContable> comprasWriter,
            ItemWriter<AsientoContable> ajustesWriter) {
        
        this.writersPorTipo = new EnumMap<>(TipoAsiento.class);
        writersPorTipo.put(TipoAsiento.VENTA, ventasWriter);
        writersPorTipo.put(TipoAsiento.COMPRA, comprasWriter);
        writersPorTipo.put(TipoAsiento.AJUSTE, ajustesWriter);
    }
    
    @Override
    @Transactional
    public void write(List<? extends AsientoContable> asientos) throws Exception {
        // Agrupamos los asientos por tipo
        Map<TipoAsiento, List<AsientoContable>> asientosPorTipo = asientos.stream()
            .collect(Collectors.groupingBy(AsientoContable::getTipo));
            
        // Procesamos cada grupo con su writer correspondiente
        for (Map.Entry<TipoAsiento, List<AsientoContable>> entry : 
             asientosPorTipo.entrySet()) {
            
            ItemWriter<AsientoContable> writer = writersPorTipo.get(entry.getKey());
            if (writer != null) {
                writer.write(entry.getValue());
            } else {
                throw new IllegalStateException(
                    "No hay writer configurado para tipo: " + entry.getKey());
            }
        }
    }
}
```

3. ESCRITURA CON BUFFER Y FLUSH CONTROLADO
----------------------------------------

Para optimizar el rendimiento, podemos implementar un sistema de buffer que acumule asientos hasta alcanzar un umbral:

```java
@Component
public class BufferedAccountingWriter implements ItemWriter<AsientoContable> {
    
    private final AsientoContableRepository repository;
    private final int bufferSize;
    private final List<AsientoContable> buffer;
    private final Object bufferLock = new Object();
    
    public BufferedAccountingWriter(
            AsientoContableRepository repository,
            @Value("${batch.writer.buffer-size:1000}") int bufferSize) {
        this.repository = repository;
        this.bufferSize = bufferSize;
        this.buffer = new ArrayList<>(bufferSize);
    }
    
    @Override
    @Transactional
    public void write(List<? extends AsientoContable> asientos) throws Exception {
        synchronized (bufferLock) {
            buffer.addAll(asientos);
            
            if (buffer.size() >= bufferSize) {
                flush();
            }
        }
    }
    
    @Scheduled(fixedDelay = 60000) // Flush cada minuto
    public void scheduledFlush() {
        synchronized (bufferLock) {
            if (!buffer.isEmpty()) {
                flush();
            }
        }
    }
    
    private void flush() {
        try {
            repository.saveAll(buffer);
            buffer.clear();
        } catch (Exception e) {
            throw new BatchBufferFlushException(
                "Error al hacer flush del buffer", e);
        }
    }
}
```

4. MANEJO DE ERRORES AVANZADO
----------------------------

Implementemos un sistema robusto de manejo de errores:

```java
@Component
@Slf4j
public class ResilientAccountingWriter implements ItemWriter<AsientoContable> {
    
    private final ItemWriter<AsientoContable> delegate;
    private final ErrorRepository errorRepository;
    private final NotificacionService notificacionService;
    
    @Override
    public void write(List<? extends AsientoContable> asientos) throws Exception {
        List<AsientoContable> asientosPendientes = new ArrayList<>(asientos);
        List<ErrorEscritura> errores = new ArrayList<>();
        
        while (!asientosPendientes.isEmpty() && errores.size() < 3) {
            try {
                delegate.write(asientosPendientes);
                break; // Si llegamos aquí, todo se escribió correctamente
            } catch (Exception e) {
                // Identificamos el asiento problemático
                AsientoContable asientoProblematico = 
                    identificarAsientoProblematico(asientosPendientes, e);
                
                // Registramos el error
                ErrorEscritura error = new ErrorEscritura(
                    asientoProblematico, e);
                errorRepository.save(error);
                errores.add(error);
                
                // Notificamos si es necesario
                if (errores.size() >= 3) {
                    notificacionService.notificarErroresConsecutivos(errores);
                }
                
                // Removemos el asiento problemático y continuamos
                asientosPendientes.remove(asientoProblematico);
            }
        }
    }
}
```

Estos patrones avanzados nos permiten:
- Escribir en múltiples destinos de manera transaccional
- Clasificar y rutear asientos según su naturaleza
- Optimizar el rendimiento con buffering
- Manejar errores de manera robusta y recuperable

Con esto concluimos el Módulo 2, que nos ha preparado para manejar lecturas y escrituras complejas en nuestro sistema contable. En el próximo módulo, veremos cómo procesar estos datos de manera efectiva.