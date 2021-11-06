package com.demo.project84;

import java.io.BufferedReader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class Main implements CommandLineRunner {
    private static final String BASE_PATH = "/Users/asurendra/code/pet/project84/";

    final OrderRepo orderRepo;
    final MaterialRepo materialRepo;
    final ProcessedRepo processedRepo;
    final BonusRepo bonusRepo;
    final FactoryRepo factoryRepo;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String caseType = args[0];
        switch (caseType) {
            case "STAGE1":
                stage1();
                break;
            case "STAGE2":
                stage2();
                break;
            case "STAGE3":
                stage3();
                break;
            case "STAGE4":
                stage4();
                break;
            case "STAGE5":
                stage5();
                break;
            case "STAGE6":
                stage6();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + caseType);
        }
    }

    /**
     * Load order file to db.
     */
    @SneakyThrows
    private void stage1() {
        log.info("Loading orders to db");
        try {
            orderRepo.deleteAll();
            Path path = Paths.get(BASE_PATH + "order-file.txt");
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                while (reader.ready()) {
                    String line = reader.readLine();
                    log.info(line);
                    String[] split = line.split(",");
                    OrderDetail order = OrderDetail.builder()
                            .color(split[0])
                            .quantity(Double.valueOf(split[1]))
                            .city(split[2])
                            .salesRep(split[3])
                            .orderDate(LocalDate.parse(split[4]))
                            .build();
                    orderRepo.save(order);
                }
            }
            log.info("Loading orders completed");
        } catch (Exception ex) {
            log.error("ERROR: stage1", ex);
            throw new RuntimeException("ERROR: stage1");
        }
    }

    /**
     * Load material file to db.
     */
    @SneakyThrows
    private void stage2() {
        log.info("Loading materials to db");
        try {
            materialRepo.deleteAll();
            Path path = Paths.get(BASE_PATH + "material-file.txt");
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                while (reader.ready()) {
                    String line = reader.readLine();
                    log.info(line);
                    String[] split = line.split(",");
                    MaterialDetail material = MaterialDetail.builder()
                            .color(split[0])
                            .quantity(Double.valueOf(split[1]))
                            .orderDate(LocalDate.parse(split[2]))
                            .build();
                    materialRepo.save(material);
                }
            }
            log.info("Loading orders completed");
        } catch (Exception ex) {
            log.error("ERROR: stage2", ex);
            throw new RuntimeException("ERROR: stage2");
        }
    }

    /**
     * Process orders if it can be fulfilled.
     */
    private void stage3() {
        log.info("Processing orders");
        try {
            processedRepo.deleteAll();
            bonusRepo.deleteAll();
            factoryRepo.deleteAll();
            Map<String, Double> cache = new HashMap<>();
            materialRepo.findAll().forEach(m -> {
                cache.put(m.getColor(), m.getQuantity());
            });
            Map<String, Double> result = new HashMap<>();
            List<OrderDetail> orders = orderRepo.findAll();
            for (OrderDetail order : orders) {
                Double balance = cache.get(order.getColor());
                if (order.getQuantity() < balance) {
                    balance = balance - order.getQuantity();
                    cache.put(order.getColor(), balance);
                    String key = order.getColor() + ":" + order.getCity();
                    Double count = result.containsKey(key) ? result.get(key) + order.getQuantity() : order.getQuantity();
                    result.put(key, count);
                    //add to processed.
                } else {
                    log.info("ERROR: stage3, will not be able to complete all order!");
                    throw new RuntimeException("ERROR: stage3, will not be able to complete all order!");
                }
            }
            result.forEach((k, v) -> {
                String[] split = k.split("\\:");
                processedRepo.save(ProcessedDetail.builder()
                        .color(split[0])
                        .quantity(v)
                        .processDate(LocalDate.now())
                        .city(split[1])
                        .build());
            });
            log.info("Processing orders completed");
        } catch (Exception ex) {
            log.error("ERROR: stage3", ex);
            throw new RuntimeException("ERROR: stage3");
        }
    }

    /**
     * Add buffer to order quantity to ensure no shortage.
     */
    private void stage4() {
        log.info("Adding buffer");
        try {
            factoryRepo.deleteAll();
            List<ProcessedDetail> processedDetail = processedRepo.findAll();
            processedDetail.forEach(p -> {
                FactoryDetail factory = FactoryDetail.builder()
                        .color(p.getColor())
                        .city(p.getCity())
                        .processDate(LocalDate.now())
                        .build();
                if (p.getQuantity() > 500) {
                    factory.setQuantity(p.getQuantity() + (p.getQuantity() * 0.30));
                } else if (p.getQuantity() > 200) {
                    factory.setQuantity(p.getQuantity() + (p.getQuantity() * 0.20));
                } else if (p.getQuantity() > 100) {
                    factory.setQuantity(p.getQuantity() + (p.getQuantity() * 0.10));
                    p.setQuantity(p.getQuantity() + (p.getQuantity() * 0.10));
                } else {
                    p.setQuantity(p.getQuantity());
                }
                factoryRepo.save(factory);

            });
            log.info("Adding buffer completed");
        } catch (Exception ex) {
            log.error("ERROR: stage4", ex);
            throw new RuntimeException("ERROR: stage4");
        }
    }

    /**
     * Add bonus points for sales rep.
     */
    private void stage5() {
        log.info("Adding Sales bonus");
        try {
            bonusRepo.deleteAll();
            Map<String, Double> result = new HashMap<>();
            List<OrderDetail> orders = orderRepo.findAll();
            for (OrderDetail order : orders) {
                String key = order.getSalesRep();
                Double count = result.containsKey(key) ? result.get(key) + order.getQuantity() : order.getQuantity();
                result.put(key, count);
            }

            result.forEach((k, v) -> {
                if (v > 200) {
                    bonusRepo.save(BonusDetail.builder()
                            .salesRep(k)
                            .bonusPoints(5)
                            .orderDate(LocalDate.now())
                            .build());
                }
                if (v > 500) {
                    bonusRepo.save(BonusDetail.builder()
                            .salesRep(k)
                            .bonusPoints(15)
                            .orderDate(LocalDate.now())
                            .build());
                }
            });
            log.info("Adding Sales bonus completed");
        } catch (Exception ex) {
            log.error("ERROR: stage5", ex);
            throw new RuntimeException("ERROR: stage5");
        }
    }

    /**
     * Notify factory to start production.
     */
    private void stage6() {
        log.info("Notifying factory");
        try {
            List<ProcessedDetail> processedDetail = processedRepo.findAll();
            processedDetail.forEach(p -> {
                log.info("Notifiying factory: {}", p);
            });
            log.info("Notifying factory completed");
        } catch (Exception ex) {
            log.error("ERROR: stage6", ex);
            throw new RuntimeException("ERROR: stage6");
        }
    }
}

interface BonusRepo extends JpaRepository<BonusDetail, Long> {
}

interface MaterialRepo extends JpaRepository<MaterialDetail, Long> {
}

interface OrderRepo extends JpaRepository<OrderDetail, Long> {
}

interface ProcessedRepo extends JpaRepository<ProcessedDetail, Long> {
}

interface FactoryRepo extends JpaRepository<FactoryDetail, Long> {
}

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class BonusDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    private String salesRep;
    private Integer bonusPoints;
    private LocalDate orderDate;
}

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class MaterialDetail implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    private String color;
    private Double quantity;
    private LocalDate orderDate;

}

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    private String color;
    private Double quantity;
    private String city;
    private String salesRep;
    private LocalDate orderDate;
}

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class ProcessedDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    private String color;
    private Double quantity;
    private String city;
    private LocalDate processDate;
}

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class FactoryDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    private String color;
    private Double quantity;
    private String city;
    private LocalDate processDate;
}
