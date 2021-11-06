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

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String caseType = args[0];
        Boolean status = false;
        switch (caseType) {
            case "STAGE1":
                status = stage1();
                break;
            case "STAGE2":
                status = stage2();
                break;
            case "STAGE3":
                status = stage3();
                break;
            case "STAGE4":
                status = stage4();
                break;
            case "STAGE5":
                status = stage5();
                break;
            case "STAGE6":
                status = stage6();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + caseType);
        }
        if (!status) {
            System.exit(1);
        }
    }


    @SneakyThrows
    private Boolean stage1() {
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
            return true;
        } catch (Exception ex) {
            log.error("ERROR: stage1", ex);
            return false;
        }
    }

    @SneakyThrows
    private Boolean stage2() {
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
            return true;
        } catch (Exception ex) {
            log.error("ERROR: stage2", ex);
            return false;
        }
    }

    private Boolean stage3() {
        log.info("Processing orders");
        try {
            processedRepo.deleteAll();
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
                    return false;
                }
            }
            result.forEach((k, v) -> {
                String[] split = k.split("\\:");
                processedRepo.save(Processed.builder()
                        .color(split[0])
                        .quantity(v)
                        .processDate(LocalDate.now())
                        .city(split[1])
                        .build());
            });
            log.info("Processing orders completed");
            return true;
        } catch (Exception ex) {
            log.error("ERROR: stage3", ex);
            return false;
        }
    }

    private Boolean stage4() {
        log.info("Adding buffer");
        try {
            List<Processed> processed = processedRepo.findAll();
            processed.forEach(p -> {
                if (!p.isBufferAdded()) {
                    if (p.getQuantity() > 500) {
                        p.setQuantity(p.getQuantity() + (p.getQuantity() * 0.30));
                        p.setBufferAdded(true);
                    } else if (p.getQuantity() > 200) {
                        p.setQuantity(p.getQuantity() + (p.getQuantity() * 0.20));
                        p.setBufferAdded(true);
                    } else if (p.getQuantity() > 100) {
                        p.setQuantity(p.getQuantity() + (p.getQuantity() * 0.10));
                        p.setBufferAdded(true);
                    }
                    processedRepo.save(p);
                }

            });
            log.info("Adding buffer completed");
            return true;
        } catch (Exception ex) {
            log.error("ERROR: stage4", ex);
            return false;
        }
    }

    private Boolean stage5() {
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
                    bonusRepo.save(Bonus.builder()
                            .salesRep(k)
                            .bonusPoints(5)
                            .orderDate(LocalDate.now())
                            .build());
                }
                if (v > 500) {
                    bonusRepo.save(Bonus.builder()
                            .salesRep(k)
                            .bonusPoints(15)
                            .orderDate(LocalDate.now())
                            .build());
                }
            });
            log.info("Adding Sales bonus completed");
            return true;
        } catch (Exception ex) {
            log.error("ERROR: stage5", ex);
            return false;
        }
    }

    private Boolean stage6() {
        log.info("Notifying factory");
        try {
            List<Processed> processed = processedRepo.findAll();
            processed.forEach(p -> {
                log.info("Notifiying factory: {}", p);
            });
            log.info("Notifying factory completed");
            return true;
        } catch (Exception ex) {
            log.error("ERROR: stage6", ex);
            return false;
        }
    }
}

interface BonusRepo extends JpaRepository<Bonus, Long> {
}

interface MaterialRepo extends JpaRepository<MaterialDetail, Long> {
}

interface NotifyRepo extends JpaRepository<Notify, Long> {
}

interface OrderRepo extends JpaRepository<OrderDetail, Long> {
}

interface ProcessedRepo extends JpaRepository<Processed, Long> {
}

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class Bonus {
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
class Notify {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    private String salesRep;
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
class Processed {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;
    private String color;
    private Double quantity;
    private String city;
    private LocalDate processDate;
    @Builder.Default
    private boolean bufferAdded = false;
}
