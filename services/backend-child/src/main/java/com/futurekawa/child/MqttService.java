package com.futurekawa.child;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttService {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    private final String broker;
    private final int entrepotId;
    private final int intervalSeconds;
    private final CapteurRepository capteurRepository;

    private final AtomicReference<Double> latestTemperature = new AtomicReference<>();
    private final AtomicReference<Double> latestHumidite = new AtomicReference<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private MqttClient mqttClient;

    public MqttService(
            @Value("${app.mqtt.broker:tcp://localhost:1883}") String broker,
            @Value("${app.entrepot-id:1}") int entrepotId,
            @Value("${app.mqtt.interval-seconds:300}") int intervalSeconds,
            CapteurRepository capteurRepository) {
        this.broker = broker;
        this.entrepotId = entrepotId;
        this.intervalSeconds = intervalSeconds;
        this.capteurRepository = capteurRepository;
    }

    @PostConstruct
    public void start() {
        try {
            mqttClient = new MqttClient(broker, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT connexion perdue: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleMessage(topic, new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            mqttClient.connect(options);
            mqttClient.subscribe("capteur/" + entrepotId + "/temperature");
            mqttClient.subscribe("capteur/" + entrepotId + "/humidite");
            log.info("MQTT connecte au broker {} pour l'entrepot {}", broker, entrepotId);

            scheduler.scheduleAtFixedRate(this::persistSnapshot, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Impossible de se connecter au broker MQTT {}: {}", broker, e.getMessage());
        }
    }

    private void handleMessage(String topic, String payload) {
        try {
            double value = Double.parseDouble(payload.trim());
            if (topic.endsWith("/temperature")) {
                latestTemperature.set(value);
                log.debug("Temperature recue: {}", value);
            } else if (topic.endsWith("/humidite")) {
                latestHumidite.set(value);
                log.debug("Humidite recue: {}", value);
            }
        } catch (NumberFormatException e) {
            log.warn("Payload invalide sur le topic {}: '{}'", topic, payload);
        }
    }

    void persistSnapshot() {
        Double temp = latestTemperature.get();
        Double hum = latestHumidite.get();
        if (temp == null || hum == null) {
            log.debug("Snapshot ignore: en attente des deux valeurs (temp={}, hum={})", temp, hum);
            return;
        }
        try {
            capteurRepository.insert(hum, temp, entrepotId);
            log.info("Snapshot persiste: temperature={} humidite={} entrepot={}", temp, hum, entrepotId);
        } catch (Exception e) {
            log.error("Echec de la persistance du snapshot: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la deconnexion MQTT: {}", e.getMessage());
        }
    }
}
