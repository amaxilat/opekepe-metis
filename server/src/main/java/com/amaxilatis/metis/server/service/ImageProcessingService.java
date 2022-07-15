package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.server.model.ImageProcessingTask;
import com.amaxilatis.metis.server.model.PoolInfo;
import com.amaxilatis.metis.server.model.TestDescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {
    
    private final FileService fileService;
    private final SimpMessagingTemplate webSocketService;
    
    private final ThreadPoolTaskExecutor taskExecutor;
    
    private final SortedSet<TestDescription> testDescriptions = new TreeSet<>();
    
    @PostConstruct
    public void init() {
        
        testDescriptions.add(TestDescription.builder().id(1).name("Έλεγχος 1: Χωρική Ανάλυση").enabled(true).description("Έλεγχος της χωρικής ανάλυσης όπου θα διαπιστωθεί ότι ο λόγος της τελικής ανάλυσης της ορθοαναγωγής προς την απόσταση δειγματοληψίας εδάφους (απόσταση μεταξύ δύο διαδοχικών κέντρων εικονοστοιχείων που μετριούνται στο έδαφος) είναι σύμφωνα με τις προδιαγραφές").build());
        testDescriptions.add(TestDescription.builder().id(2).name("Έλεγχος 2: Ραδιομετρική Ανάλυση").enabled(true).description("Έλεγχος της ραδιομετρικής ανάλυσης όπου θα επαληθευτεί ότι είναι 11-12 bits ανά κανάλι σύμφωνα με τις προδιαγραφές").build());
        testDescriptions.add(TestDescription.builder().id(3).name("Έλεγχος 3: Φασματική Ανάλυση").enabled(true).description("Έλεγχος της φασματικής ανάλυσης όπου θα διαπιστωθεί ότι το πλήθος των καναλιών είναι σύμφωνο με τα στοιχεία παράδοσης και της προδιαγραφές").build());
        testDescriptions.add(TestDescription.builder().id(4).name("Έλεγχος 4: Νεφοκάλυψη").enabled(false).description("Έλεγχος νεφοκάλυψης ανά εικόνα και συνολικά σε συμφωνία με τις προδιαγραφές").build());
        testDescriptions.add(TestDescription.builder().id(5).name("Έλεγχος 5: Ολικό clipping").enabled(true).description("Έλεγχος ολικού clipping το οποίο υπολογίζεται στο ιστόγραμμα φωτεινότητας σύμφωνα με τις προδιαγραφές").build());
        testDescriptions.add(TestDescription.builder().id(6).name("Έλεγχος 6: Κορυφής Ιστογράμματος").enabled(true).description("Έλεγχος κορυφής ιστογράμματος από την τυπική μέση τιμή (πχ 8bit 128) και σύμφωνα με τις προδιαγραφές").build());
        testDescriptions.add(TestDescription.builder().id(7).name("Έλεγχος 7: Αντίθεσης").enabled(true).description("Έλεγχος αντίθεσης ανά κανάλι ως έλεγχος της μεταβλητότητας των ψηφιακών τιμών (DN) σαν ποσοστό των διαθεσίμων επιπέδων του γκρι και σύμφωνα με τις προδιαγραφές").build());
        testDescriptions.add(TestDescription.builder().id(8).name("Έλεγχος 8: Συμπίεσης").enabled(true).description("Έλεγχος συμπίεσης στον μορφότυπο των αρχείων (GeoTiff ή/και JPEG2000) και σύμφωνα με τις προδιαγραφές").build());
        testDescriptions.add(TestDescription.builder().id(9).name("Έλεγχος 9: Ομοιογενών Αντικειμένων").enabled(false).description("Αναγνώριση ομοιογενών αντικειμένων και αυτόματη μέτρηση και για την ισορροπία χρώματος και θόρυβο όπου προκύπτει αφενός ως η διαφορά μεταξύ του ελάχιστου και του μέγιστου ψηφιακού συνόλου στην τριάδα υπολογιζόμενη σε σχεδόν «ουδέτερα» αντικείμενα (όπως άσφαλτος ή ταράτσες κτιρίων - δεν εφαρμόζεται σε παγχρωματικές εικόνες) και αφετέρου ως η αναλογία σήματος προς θόρυβο (SNR) που καθορίζεται σαν τον λόγο της μέσης ψηφιακής τιμής (DN) του pixel (DN Value) προς την μεταβλητότητα (standard deviation) των ψηφιακών τιμών (υπολογισμένη σε περιοχές με ομοιόμορφη πυκνότητα μέσων τιμών) και σύμφωνα με τις προδιαγραφές").build());
    }
    
    @Scheduled(fixedRate = 10000L)
    public void logPool() {
        final long pending = taskExecutor.getThreadPoolExecutor().getTaskCount() - taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
        log.debug("[pool] size:{} active:{} pending:{}", taskExecutor.getPoolSize(), taskExecutor.getActiveCount(), pending);
    }
    
    @Scheduled(fixedRate = 1000L)
    public void updatePool() {
        webSocketService.convertAndSend("/topic/pool", getPoolInfo());
    }
    
    public void processFile(final String outFileName, final String filename, final List<Integer> tasks) {
        taskExecutor.execute(new ImageProcessingTask(fileService, outFileName, filename, tasks));
    }
    
    public PoolInfo getPoolInfo() {
        final long pending = taskExecutor.getThreadPoolExecutor().getTaskCount() - taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
        return new PoolInfo(taskExecutor.getCorePoolSize(), taskExecutor.getPoolSize(), taskExecutor.getActiveCount(), pending);
    }
    
    public Set<TestDescription> getTestDescriptions() {
        return testDescriptions;
    }
}
