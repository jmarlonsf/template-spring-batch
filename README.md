# Template Spring Batch

Template completo de projeto Spring Boot com Spring Batch, demonstrando diferentes estratégias de processamento ETL com PostgreSQL.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Configuração](#configuração)
- [Jobs Disponíveis](#jobs-disponíveis)
- [Como Executar](#como-executar)
- [Arquitetura](#arquitetura)
- [Boas Práticas](#boas-práticas)
- [Exemplos](#exemplos)

---

## 🎯 Visão Geral

Este projeto é um template completo que demonstra diferentes estratégias de processamento batch usando Spring Batch:

1. **Processamento Simples**: Jobs independentes para processar tabelas separadas
2. **JOIN Direto**: JOIN SQL otimizado no Reader
3. **JOIN Posterior**: Processamento em etapas com staging tables

### Objetivos

- Demonstrar boas práticas de Spring Batch
- Mostrar diferentes estratégias de ETL
- Explicar decisões arquiteturais
- Fornecer template reutilizável

---

## 🛠 Tecnologias

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Batch** (processamento batch)
- **Spring JDBC** (acesso a dados)
- **PostgreSQL** (banco de dados)
- **Maven** (gerenciamento de dependências)

---

## 📁 Estrutura do Projeto

```
src/main/java/com/template/batch/
├── config/
│   ├── BatchConfig.java              # Configuração básica do Spring Batch
│   ├── JobConfig.java                 # Definição dos Jobs
│   ├── StepConfig.java                # Definição dos Steps
│   ├── ReaderConfig.java              # Configuração dos Readers
│   ├── WriterConfig.java              # Configuração dos Writers
│   ├── MergedRecordReaderConfig.java  # Reader customizado para merge
│   ├── MergedRecordItemReader.java    # Implementação do reader com ItemStream
│   └── JobLauncherRunner.java         # Runner para executar jobs
│
├── domain/
│   ├── SourceRecord.java              # Modelo para tabelas de origem
│   ├── TargetRecord.java              # Modelo para tabela de destino
│   ├── JoinedSourceRecord.java       # DTO para resultado de JOIN SQL
│   └── MergedRecord.java              # DTO para resultado de merge via staging
│
├── processor/
│   ├── CommonItemProcessor.java       # Processor para SourceRecord → TargetRecord
│   ├── JoinedSourceRecordProcessor.java # Processor para JOIN direto
│   ├── MergedRecordProcessor.java     # Processor para merge final
│   └── PassThroughProcessor.java      # Processor pass-through (sem transformação)
│
├── listener/
│   └── BatchExecutionListener.java    # Listeners para logging
│
└── SpringBatchApplication.java        # Classe principal

src/main/resources/
├── application.yml                     # Configurações da aplicação
├── schema.sql                          # Schema do banco de dados
└── data.sql                            # Dados de exemplo
```

---

## ⚙️ Configuração

### Pré-requisitos

1. **Java 17+**
2. **PostgreSQL** rodando na porta 5432
3. **Maven 3.6+**

### Setup do Banco de Dados

1. Crie o banco de dados:
```sql
CREATE DATABASE spring_batch;
```

2. Configure as credenciais no `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/spring_batch
    username: postgres
    password: postgres
```

3. O schema será criado automaticamente na primeira execução:
   - Tabelas de origem: `source_table_a`, `source_table_b`
   - Tabelas de staging: `staging_table_a`, `staging_table_b`
   - Tabela de destino: `target_table`
   - Tabelas do Spring Batch: `BATCH_*` (criadas automaticamente)

---

## 🚀 Jobs Disponíveis

### 1. `jobA` - Processamento Simples (Tabela A)

**Descrição**: Processa dados apenas da `source_table_a`.

**Fluxo**:
```
source_table_a → SourceRecord → TargetRecord → target_table
```

**Quando usar**:
- Processar dados de uma única tabela
- Dados independentes que não precisam de merge
- Processamento simples e direto

**Executar**:
```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=jobA"
```

---

### 2. `jobB` - Processamento Simples (Tabela B)

**Descrição**: Processa dados apenas da `source_table_b`.

**Fluxo**:
```
source_table_b → SourceRecord → TargetRecord → target_table
```

**Quando usar**:
- Processar dados de uma única tabela
- Dados independentes que não precisam de merge
- Processamento simples e direto

**Executar**:
```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=jobB"
```

---

### 3. `joinDirectJob` - JOIN Direto no SQL

**Descrição**: Faz JOIN direto entre `source_table_a` e `source_table_b` no SQL do Reader.

**Fluxo**:
```
JOIN(source_table_a, source_table_b) → JoinedSourceRecord → TargetRecord → target_table
```

**Características**:
- ✅ JOIN otimizado pelo banco (100-1000x mais rápido)
- ✅ Streaming via cursor (baixo uso de memória)
- ✅ Restartability automática
- ✅ Snapshot transacional consistente

**Quando usar**:
- ✅ Dados relacionados que precisam ser combinados
- ✅ Grande volume de dados (milhões de registros)
- ✅ Performance crítica
- ✅ Processamento longo que precisa de restartability
- ❌ Não usar quando dados são independentes

**Executar**:
```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=joinDirectJob"
```

---

### 4. `joinStagingJob` - JOIN Posterior via Staging

**Descrição**: Processa dados em 3 etapas usando staging tables para flexibilidade.

**Fluxo**:
```
Step 1: source_table_a → staging_table_a
Step 2: source_table_b → staging_table_b
Step 3: staging_table_a + staging_table_b → target_table (merge)
```

**Características**:
- ✅ Reprocessamento seletivo (apenas step necessário)
- ✅ Validação intermediária (inspecionar staging)
- ✅ Flexibilidade no reprocessamento
- ✅ Isolamento de dados (origens intactas)

**Quando usar**:
- ✅ Precisa de reprocessamento seletivo
- ✅ Precisa validar dados antes do merge
- ✅ Processamento longo que pode falhar no merge
- ✅ ETL complexo com múltiplas etapas
- ❌ Não usar quando processamento simples é suficiente

**Executar**:
```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=joinStagingJob"
```

---

## 🏃 Como Executar

### Executar um Job Específico

```powershell
# Job A
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=jobA"

# Job B
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=jobB"

# JOIN Direto
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=joinDirectJob"

# JOIN via Staging
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=joinStagingJob"
```

### Executar via JAR

```powershell
# Compilar
mvn clean package

# Executar
java -jar target/template-spring-batch-1.0.0.jar --spring.batch.job.name=joinStagingJob
```

### Verificar Resultados

```sql
-- Ver dados processados
SELECT * FROM target_table ORDER BY id;

-- Ver execuções do Spring Batch
SELECT 
    ji.JOB_NAME,
    je.STATUS,
    je.EXIT_CODE,
    se.READ_COUNT,
    se.WRITE_COUNT
FROM BATCH_JOB_INSTANCE ji
JOIN BATCH_JOB_EXECUTION je ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
JOIN BATCH_STEP_EXECUTION se ON je.JOB_EXECUTION_ID = se.JOB_EXECUTION_ID
WHERE ji.JOB_NAME = 'joinStagingJob'
ORDER BY je.JOB_EXECUTION_ID DESC
LIMIT 1;
```

---

## 🏗 Arquitetura

### Princípios de Design

1. **Separação de Responsabilidades**
   - Reader: Aquisição de dados (I/O)
   - Processor: Transformação e regras de negócio (lógica)
   - Writer: Persistência (I/O)

2. **Push Processing to the Database**
   - JOINs, filtros e transformações simples no SQL
   - Aproveita otimizações do banco
   - Reduz I/O e uso de memória

3. **Streaming First**
   - Uso de cursors para processar grandes volumes
   - Não carrega tudo na memória
   - Eficiente para milhões de registros

### Decisões Arquiteturais

#### JOIN no Reader vs Processor

**✅ JOIN no Reader (SQL)**:
- Performance: 100-1000x mais rápido
- Memória: O(chunk_size) vs O(N)
- Restartability: Gerenciada automaticamente
- Consistência: Snapshot transacional

**❌ JOIN no Processor**:
- Performance: Lento (N x M comparações)
- Memória: Precisa carregar tabelas inteiras
- Restartability: Precisa implementar manualmente
- Consistência: Pode ter race conditions

#### Staging Tables

**Vantagens**:
- Reprocessamento seletivo
- Validação intermediária
- Isolamento de dados
- Flexibilidade

**Quando usar**:
- Processamento complexo em múltiplas etapas
- Necessidade de validação antes do merge
- Processamento longo que pode falhar

---

## 📚 Boas Práticas

### 1. Onde Fazer Transformações?

**✅ No SQL (Reader)**:
- Filtros (WHERE)
- Transformações simples (UPPER, LOWER, TRIM)
- Cálculos matemáticos simples
- JOINs e agregações
- Performance crítica

**✅ No Processor (Java)**:
- Lógica de negócio complexa
- Validações que dependem de múltiplos campos
- Chamadas a serviços externos
- Dados dinâmicos (LocalDateTime.now(), UUID)
- Regras que mudam frequentemente

### 2. Streaming vs Full Load

**✅ Streaming (JdbcCursorItemReader)**:
- Processa milhões de registros
- Baixo uso de memória
- Restartability automática
- Recomendado para grandes volumes

**❌ Full Load**:
- Carrega tudo na memória
- Pode causar OutOfMemoryError
- Não recomendado para grandes volumes

### 3. UPSERT vs INSERT

**✅ UPSERT (ON CONFLICT DO UPDATE)**:
- Idempotente (pode executar múltiplas vezes)
- Facilita retry e reprocessamento
- Recomendado para Spring Batch

**❌ INSERT simples**:
- Pode causar erros de chave duplicada
- Não é idempotente
- Não recomendado

---

## 📊 Exemplos

### Exemplo 1: Processamento Simples

```powershell
# Executar jobA
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=jobA"

# Resultado: dados de source_table_a processados e salvos em target_table
```

### Exemplo 2: JOIN Direto

```powershell
# Executar joinDirectJob
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=joinDirectJob"

# Resultado: dados combinados de source_table_a e source_table_b
# salvos em target_table com valor = valueA + valueB
```

### Exemplo 3: JOIN via Staging

```powershell
# Executar joinStagingJob
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=joinStagingJob"

# Resultado:
# Step 1: source_table_a → staging_table_a
# Step 2: source_table_b → staging_table_b
# Step 3: staging tables → target_table (merge)
```

### Exemplo 4: Reprocessamento Seletivo

```sql
-- Se Step 3 falhar, apenas reprocessar o merge:
-- Dados já estão em staging, não precisa reprocessar Step 1 e 2

-- Executar apenas Step 3 (criar job separado ou usar Spring Batch Admin)
```

---

## 🔍 Monitoramento

### Logs

O projeto está configurado com logging detalhado:

```yaml
logging:
  level:
    org.springframework.batch: DEBUG
    org.springframework.jdbc: DEBUG
    com.template: DEBUG
```

### Métricas do Spring Batch

```sql
-- Ver histórico de execuções
SELECT * FROM BATCH_JOB_EXECUTION 
WHERE JOB_INSTANCE_ID IN (
    SELECT JOB_INSTANCE_ID FROM BATCH_JOB_INSTANCE 
    WHERE JOB_NAME = 'joinStagingJob'
)
ORDER BY JOB_EXECUTION_ID DESC;

-- Ver detalhes do step
SELECT * FROM BATCH_STEP_EXECUTION
WHERE JOB_EXECUTION_ID = ?;
```

---

## 🧪 Estrutura de Dados

### Tabelas de Origem

```sql
source_table_a:
  - id (BIGINT PRIMARY KEY)
  - nome (VARCHAR(100))
  - valor (DECIMAL(10,2))

source_table_b:
  - id (BIGINT PRIMARY KEY)
  - nome (VARCHAR(100))
  - valor (DECIMAL(10,2))
```

### Tabelas de Staging

```sql
staging_table_a:
  - id (BIGINT PRIMARY KEY)
  - name (VARCHAR(100))
  - value (DECIMAL(10,2))

staging_table_b:
  - id (BIGINT PRIMARY KEY)
  - name (VARCHAR(100))
  - value (DECIMAL(10,2))
```

### Tabela de Destino

```sql
target_table:
  - id (BIGINT PRIMARY KEY)
  - nome (VARCHAR(100))
  - valor (DECIMAL(10,2))
  - processedo_em (TIMESTAMP)
```

---

## 🎓 Conceitos Importantes

### ItemStream

Readers customizados que encapsulam outros readers devem implementar `ItemStream` para gerenciar o ciclo de vida:

```java
public class MergedRecordItemReader extends ItemStreamSupport 
    implements ItemReader<MergedRecord> {
    
    @Override
    public void open(ExecutionContext executionContext) {
        // Abre reader interno
    }
    
    @Override
    public void update(ExecutionContext executionContext) {
        // Salva estado do reader interno
    }
    
    @Override
    public void close() {
        // Fecha reader interno
    }
}
```

### Restartability

Spring Batch gerencia automaticamente o estado dos jobs:

- Salva posição do cursor
- Permite reiniciar de onde parou
- Funciona mesmo se dados mudarem entre execuções

### Chunk Processing

Processa dados em chunks (lotes):

- Chunk size: 10 (configurável)
- Commit por chunk (transacional)
- Rollback em caso de erro

---

## 📖 Referências

- [Spring Batch Documentation](https://docs.spring.io/spring-batch/docs/current/reference/html/)
- [Spring Boot Batch](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.batch)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

## 📝 Licença

Este é um template de código para projetos internos.

---

## 👥 Contribuindo

Este é um template de referência. Sinta-se livre para adaptar às necessidades do seu projeto.

---

**Última atualização**: Janeiro 2026
