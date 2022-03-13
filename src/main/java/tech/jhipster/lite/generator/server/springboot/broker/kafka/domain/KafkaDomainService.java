package tech.jhipster.lite.generator.server.springboot.broker.kafka.domain;

import static tech.jhipster.lite.common.domain.FileUtils.getPath;
import static tech.jhipster.lite.generator.project.domain.Constants.MAIN_JAVA;
import static tech.jhipster.lite.generator.project.domain.Constants.TEST_JAVA;
import static tech.jhipster.lite.generator.project.domain.DefaultConfig.*;

import java.util.TreeMap;
import tech.jhipster.lite.error.domain.GeneratorException;
import tech.jhipster.lite.generator.buildtool.generic.domain.BuildToolService;
import tech.jhipster.lite.generator.buildtool.generic.domain.Dependency;
import tech.jhipster.lite.generator.project.domain.DefaultConfig;
import tech.jhipster.lite.generator.project.domain.Project;
import tech.jhipster.lite.generator.project.domain.ProjectRepository;
import tech.jhipster.lite.generator.server.springboot.common.domain.SpringBootCommonService;

public class KafkaDomainService implements KafkaService {

  public static final String SOURCE = "server/springboot/broker/kafka";
  public static final String DUMMY_TOPIC_NAME = "kafka.topic.dummy";

  private final BuildToolService buildToolService;

  private final ProjectRepository projectRepository;

  private final SpringBootCommonService springBootCommonService;

  public KafkaDomainService(
    final BuildToolService buildToolService,
    final ProjectRepository projectRepository,
    final SpringBootCommonService springBootCommonService
  ) {
    this.buildToolService = buildToolService;
    this.projectRepository = projectRepository;
    this.springBootCommonService = springBootCommonService;
  }

  @Override
  public void init(final Project project) {
    addApacheKafkaClient(project);
    addDockerCompose(project);
    addProperties(project);
    addTestcontainers(project);
  }

  @Override
  public void addDummyProducer(final Project project) {
    if (!springBootCommonService.getProperty(project, DUMMY_TOPIC_NAME).isPresent()) {
      project.addDefaultConfig(PACKAGE_NAME);
      project.addDefaultConfig(BASE_NAME);
      String packageNamePath = project.getPackageNamePath().orElse(getPath(DefaultConfig.PACKAGE_PATH));
      String secondaryKafkaPath = "technical/infrastructure/secondary/kafka";
      String dummyProducerPath = "dummy/infrastructure/secondary/kafka/producer";

      String topicName = "queue." + project.getBaseName().orElse("jhipster") + ".dummy";
      springBootCommonService.addProperties(project, DUMMY_TOPIC_NAME, topicName);
      springBootCommonService.addPropertiesTest(project, DUMMY_TOPIC_NAME, topicName);

      projectRepository.template(project, SOURCE, "KafkaProducerProperties.java", getPath(MAIN_JAVA, packageNamePath, secondaryKafkaPath));
      projectRepository.template(
        project,
        SOURCE,
        "KafkaProducerPropertiesTest.java",
        getPath(TEST_JAVA, packageNamePath, secondaryKafkaPath)
      );
      projectRepository.template(project, SOURCE, "DummyProducer.java", getPath(MAIN_JAVA, packageNamePath, dummyProducerPath));
      projectRepository.template(project, SOURCE, "DummyProducerTest.java", getPath(TEST_JAVA, packageNamePath, dummyProducerPath));
      projectRepository.template(project, SOURCE, "KafkaConfiguration.java", getPath(MAIN_JAVA, packageNamePath, secondaryKafkaPath));
    }
  }

  private void addApacheKafkaClient(final Project project) {
    Dependency dependency = Dependency.builder().groupId("org.apache.kafka").artifactId("kafka-clients").build();
    buildToolService.addDependency(project, dependency);
  }

  private void addDockerCompose(final Project project) {
    project.addDefaultConfig(BASE_NAME);
    project.addConfig("zookeeperDockerImage", Zookeeper.getZookeeperDockerImage());
    project.addConfig("kafkaDockerImage", Kafka.getKafkaDockerImage());
    projectRepository.template(project, SOURCE, "kafka.yml", "src/main/docker", "kafka.yml");
  }

  private void addProperties(final Project project) {
    getKafkaCommonProperties()
      .forEach((k, v) -> {
        springBootCommonService.addProperties(project, k, v);
        springBootCommonService.addPropertiesTest(project, k, v);
      });
  }

  private TreeMap<String, Object> getKafkaCommonProperties() {
    TreeMap<String, Object> result = new TreeMap<>();

    result.put("# Kafka Configuration", "");
    result.put("kafka.bootstrap-servers", "localhost:9092");
    result.put("kafka.consumer.'[key.deserializer]'", "org.apache.kafka.common.serialization.StringDeserializer");
    result.put("kafka.consumer.'[value.deserializer]'", "org.apache.kafka.common.serialization.StringDeserializer");
    result.put("kafka.consumer.'[group.id]'", DASHERIZED_BASE_NAME);
    result.put("kafka.consumer.'[auto.offset.reset]'", "earliest");
    result.put("kafka.producer.'[key.serializer]'", "org.apache.kafka.common.serialization.StringSerializer");
    result.put("kafka.producer.'[value.serializer]'", "org.apache.kafka.common.serialization.StringSerializer");
    result.put("kafka.polling.timeout", "10000");

    return result;
  }

  void addTestcontainers(final Project project) {
    this.buildToolService.getVersion(project, "testcontainers")
      .ifPresentOrElse(
        version -> {
          Dependency dependency = Dependency
            .builder()
            .groupId("org.testcontainers")
            .artifactId("kafka")
            .version("\\${testcontainers.version}")
            .scope("test")
            .build();
          buildToolService.addProperty(project, "testcontainers.version", version);
          buildToolService.addDependency(project, dependency);
        },
        () -> {
          throw new GeneratorException("Testcontainers version not found");
        }
      );
  }
}
