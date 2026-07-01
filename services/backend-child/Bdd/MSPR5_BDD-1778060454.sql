DROP TABLE IF EXISTS "expedition_lot" CASCADE;
DROP TABLE IF EXISTS "capteur" CASCADE;
DROP TABLE IF EXISTS "lot" CASCADE;
DROP TABLE IF EXISTS "entrepot" CASCADE;
DROP TABLE IF EXISTS "exploitation" CASCADE;
DROP TABLE IF EXISTS "chargement" CASCADE;
DROP TABLE IF EXISTS "expedition" CASCADE;

CREATE TABLE IF NOT EXISTS "entrepot" (
	"ID_ENTREPOT" serial NOT NULL,
	"nom" varchar(255) NOT NULL,
	"longitude" double precision,
	"latitude" double precision,
	"taille" double precision,
	PRIMARY KEY ("ID_ENTREPOT")
);

CREATE TABLE IF NOT EXISTS "lot" (
	"ID_LOT" serial NOT NULL,
	"ID_ENTREPOT" integer NOT NULL,
	"ID_exploitation" integer,
	"ID_chargement" integer,
	"lot_reference" varchar(64) NOT NULL,
	"date_peremption" integer NOT NULL,
	"variete" varchar(120) NOT NULL,
	"process" varchar(40) NOT NULL,
	"score_sca" numeric(4,2),
	"poids_kg" numeric(10,2) NOT NULL,
	"qualite" varchar(255) NOT NULL,
	"quantite" integer NOT NULL,
	PRIMARY KEY ("ID_LOT")
);

CREATE TABLE IF NOT EXISTS "chargement" (
	"ID_chargement" serial NOT NULL,
	"datetime" date,
	PRIMARY KEY ("ID_chargement")
);

CREATE TABLE IF NOT EXISTS "expedition" (
	"ID_expedition" serial NOT NULL,
	"depart_at" timestamp NOT NULL,
	"arrivee_estimee_at" timestamp,
	"destination_pays" varchar(120) NOT NULL,
	"destination_ville" varchar(120) NOT NULL,
	"destination_client" varchar(160) NOT NULL,
	"poids_total_kg" numeric(10,2) NOT NULL,
	"tracking_transporteur" varchar(120),
	"quai_depart" varchar(40),
	"transporteur" varchar(120),
	"livreur_nom" varchar(120) NOT NULL,
	"livreur_telephone" varchar(40),
	"statut" varchar(40) NOT NULL,
	CONSTRAINT "expedition_statut_check" CHECK ("statut" IN ('En préparation', 'Expédiée', 'Livrée', 'En preparation', 'Expediee', 'Livree')),
	PRIMARY KEY ("ID_expedition")
);

CREATE TABLE IF NOT EXISTS "expedition_lot" (
	"ID_expedition" integer NOT NULL,
	"ID_LOT" integer NOT NULL,
	"quantite_expediee" integer NOT NULL,
	CONSTRAINT "expedition_lot_quantite_positive" CHECK ("quantite_expediee" > 0),
	PRIMARY KEY ("ID_expedition", "ID_LOT")
);

CREATE TABLE IF NOT EXISTS "capteur" (
	"ID_capteur" serial NOT NULL,
	"humidite" double precision,
	"temperature" double precision,
	"date" date,
	"ID_entrepot" integer NOT NULL,
	PRIMARY KEY ("ID_capteur")
);

CREATE TABLE IF NOT EXISTS "exploitation" (
	"ID_exploitation" serial NOT NULL,
	"longitude" double precision,
	"latitude" double precision,
	PRIMARY KEY ("ID_exploitation")
);

ALTER TABLE "lot"
ADD CONSTRAINT "lot_fk1"
FOREIGN KEY ("ID_ENTREPOT")
REFERENCES "entrepot"("ID_ENTREPOT");

ALTER TABLE "lot"
ADD CONSTRAINT "lot_fk2"
FOREIGN KEY ("ID_exploitation")
REFERENCES "exploitation"("ID_exploitation");

ALTER TABLE "lot"
ADD CONSTRAINT "lot_fk3"
FOREIGN KEY ("ID_chargement")
REFERENCES "chargement"("ID_chargement");

ALTER TABLE "capteur"
ADD CONSTRAINT "capteur_fk4"
FOREIGN KEY ("ID_entrepot")
REFERENCES "entrepot"("ID_ENTREPOT");

ALTER TABLE "expedition_lot"
ADD CONSTRAINT "expedition_lot_fk1"
FOREIGN KEY ("ID_expedition")
REFERENCES "expedition"("ID_expedition");

ALTER TABLE "expedition_lot"
ADD CONSTRAINT "expedition_lot_fk2"
FOREIGN KEY ("ID_LOT")
REFERENCES "lot"("ID_LOT");

-- Seed: un entrepot par backend fille (Bresil=1, Equateur=2, Colombie=3)
INSERT INTO "entrepot" ("ID_ENTREPOT", "nom", "longitude", "latitude", "taille") VALUES
(1, 'Entrepot Bresil',    -47.9292, -15.7801, 1000.0),
(2, 'Entrepot Equateur',  -78.5249,  -0.2295,  800.0),
(3, 'Entrepot Colombie',  -74.0721,   4.7110,  900.0);

SELECT setval(pg_get_serial_sequence('"entrepot"', 'ID_ENTREPOT'), 3);

-- Seed: exploitations
INSERT INTO "exploitation" ("ID_exploitation", "longitude", "latitude") VALUES
(1, -48.1020, -15.6520),
(2, -78.5800,  -0.2010),
(3, -74.1200,   4.6500),
(4, -47.8500, -15.8200),
(5, -78.4300,  -0.3200),
(6, -74.0000,   4.7800);

SELECT setval(pg_get_serial_sequence('"exploitation"', 'ID_exploitation'), 6);

-- Seed: chargements
INSERT INTO "chargement" ("ID_chargement", "datetime") VALUES
(1, CURRENT_DATE - 28),
(2, CURRENT_DATE - 24),
(3, CURRENT_DATE - 20),
(4, CURRENT_DATE - 16),
(5, CURRENT_DATE - 12),
(6, CURRENT_DATE - 8),
(7, CURRENT_DATE - 4),
(8, CURRENT_DATE - 2);

SELECT setval(pg_get_serial_sequence('"chargement"', 'ID_chargement'), 8);

-- Seed: expeditions
INSERT INTO "expedition" (
	"ID_expedition", "depart_at", "arrivee_estimee_at", "destination_pays", "destination_ville", "destination_client",
	"poids_total_kg", "tracking_transporteur", "quai_depart", "transporteur", "livreur_nom", "livreur_telephone", "statut"
) VALUES
(1, CURRENT_DATE - 3 + TIME '06:30', CURRENT_DATE - 2 + TIME '14:00', 'France', 'Le Havre', 'Amazon France', 486.00, 'FR-FA-2026-000781', 'Q3', 'Fret Atlantique', 'Carlos Almeida', '+33-6-11-22-33-44', 'Expédiée'),
(2, CURRENT_DATE - 2 + TIME '08:10', CURRENT_DATE - 1 + TIME '16:20', 'Allemagne', 'Hambourg', 'Hanse Kaffee GmbH', 412.00, 'DE-AC-2026-001245', 'Q1', 'Andes Cargo', 'Luis Mendoza', '+49-171-555-0110', 'Expédiée'),
(3, CURRENT_DATE - 1 + TIME '05:50', CURRENT_DATE + TIME '12:40', 'Etats-Unis', 'Miami', 'Bean Harbor Inc', 398.00, 'US-BOF-2026-004120', 'Q2', 'Blue Ocean Freight', 'Diego Ramirez', '+1-305-555-0142', 'Expédiée'),
(4, CURRENT_DATE + TIME '07:15', CURRENT_DATE + 1 + TIME '15:45', 'Canada', 'Montreal', 'Cafe Boreal', 355.00, 'CA-LL-2026-000334', 'Q4', 'Latam Logistics', 'Andres Pereira', '+1-514-555-0170', 'En préparation'),
(5, CURRENT_DATE + 1 + TIME '06:00', CURRENT_DATE + 2 + TIME '13:30', 'Espagne', 'Valence', 'Tostadora Sol', 268.00, 'ES-FA-2026-000902', 'Q5', 'Fret Atlantique', 'Rafael Costa', '+34-600-123-456', 'En préparation'),
(6, CURRENT_DATE + 2 + TIME '09:20', CURRENT_DATE + 3 + TIME '17:10', 'Japon', 'Yokohama', 'Kissa Trading', 190.00, 'JP-PG-2026-000117', 'Q6', 'Pacific Gate', 'Bruno Silva', '+81-90-5555-7788', 'Livrée');

SELECT setval(pg_get_serial_sequence('"expedition"', 'ID_expedition'), 6);

-- Seed: lots (stock reel avec statuts de qualite et peremption)
INSERT INTO "lot" (
	"ID_LOT", "ID_ENTREPOT", "ID_exploitation", "ID_chargement", "lot_reference", "date_peremption",
	"variete", "process", "score_sca", "poids_kg", "qualite", "quantite"
) VALUES
(1, 1, 1, 1, 'BR-24-0001', 20, 'Bourbon Rouge', 'Lave', 84.50, 180.00, 'A', 180),
(2, 1, 4, 2, 'BR-24-0002', 14, 'Catuai Jaune', 'Honey', 83.10, 140.00, 'A-', 140),
(3, 1, 1, 3, 'BR-24-0003', 8, 'Mundo Novo', 'Nature', 81.40, 95.00, 'B', 95),
(4, 1, 4, 4, 'BR-24-0004', 5, 'Arara', 'Anaerobie', 79.20, 60.00, 'C', 60),
(5, 2, 2, 5, 'EC-24-0001', 22, 'Typica', 'Lave', 85.20, 210.00, 'A', 210),
(6, 2, 5, 6, 'EC-24-0002', 11, 'Caturra', 'Honey', 82.90, 150.00, 'B+', 150),
(7, 2, 2, 7, 'EC-24-0003', 7, 'Sidra', 'Anaerobie', 86.10, 105.00, 'B', 105),
(8, 2, 5, 8, 'EC-24-0004', 3, 'Castillo', 'Nature', 78.90, 55.00, 'C', 55),
(9, 3, 3, 3, 'CO-24-0001', 19, 'Castillo', 'Lave', 84.00, 170.00, 'A', 170),
(10, 3, 6, 4, 'CO-24-0002', 13, 'Colombia', 'Honey', 83.70, 130.00, 'A-', 130),
(11, 3, 3, 6, 'CO-24-0003', 6, 'Geisha', 'Anaerobie', 87.40, 90.00, 'B', 90),
(12, 3, 6, 7, 'CO-24-0004', 2, 'Cenicafe 1', 'Nature', 80.10, 48.00, 'C', 48);

SELECT setval(pg_get_serial_sequence('"lot"', 'ID_LOT'), 12);

-- Seed: lots supplementaires pour enrichir le dashboard
INSERT INTO "lot" (
	"ID_LOT", "ID_ENTREPOT", "ID_exploitation", "ID_chargement", "lot_reference", "date_peremption",
	"variete", "process", "score_sca", "poids_kg", "qualite", "quantite"
) VALUES
(13, 1, 1, 5, 'BR-24-0005', 24, 'Yellow Bourbon', 'Honey', 85.90, 132.00, 'A', 132),
(14, 1, 4, 6, 'BR-24-0006', 17, 'Topazio', 'Lave', 82.60, 118.00, 'A-', 118),
(15, 1, 1, 7, 'BR-24-0007', 10, 'Obata', 'Nature', 80.50, 88.00, 'B+', 88),
(16, 1, 4, 8, 'BR-24-0008', 4, 'Icatu', 'Anaerobie', 78.40, 52.00, 'C', 52),
(17, 2, 2, 1, 'EC-24-0005', 26, 'SL28', 'Lave', 86.40, 146.00, 'A', 146),
(18, 2, 5, 2, 'EC-24-0006', 18, 'Pacamara', 'Honey', 84.10, 124.00, 'A-', 124),
(19, 2, 2, 3, 'EC-24-0007', 9, 'Bourbon Sidra', 'Anaerobie', 87.00, 93.00, 'B+', 93),
(20, 2, 5, 4, 'EC-24-0008', 5, 'Pichincha Caturra', 'Nature', 79.10, 57.00, 'C', 57),
(21, 3, 3, 5, 'CO-24-0005', 23, 'Pink Bourbon', 'Lave', 86.80, 138.00, 'A', 138),
(22, 3, 6, 6, 'CO-24-0006', 16, 'Tabi', 'Honey', 83.50, 121.00, 'A-', 121),
(23, 3, 3, 7, 'CO-24-0007', 8, 'Typica Mejorado', 'Anaerobie', 88.20, 84.00, 'B+', 84),
(24, 3, 6, 8, 'CO-24-0008', 3, 'Castillo Naranjal', 'Nature', 79.70, 50.00, 'C', 50);

SELECT setval(pg_get_serial_sequence('"lot"', 'ID_LOT'), 24);

-- Seed: composition des expeditions (quel lot dans quelle expedition)
INSERT INTO "expedition_lot" ("ID_expedition", "ID_LOT", "quantite_expediee") VALUES
(1, 1, 120),
(1, 5, 80),
(1, 13, 66),
(2, 2, 100),
(2, 9, 90),
(2, 18, 62),
(3, 3, 70),
(3, 6, 60),
(3, 10, 80),
(3, 21, 58),
(4, 7, 85),
(4, 11, 55),
(4, 14, 44),
(5, 4, 45),
(5, 8, 40),
(5, 19, 39),
(6, 12, 35);

-- Seed: mesures capteurs (12 points par entrepot pour courbes et alertes)
INSERT INTO "capteur" ("ID_capteur", "humidite", "temperature", "date", "ID_entrepot") VALUES
-- Bresil (ID 1)
(1, 54.0, 22.4, CURRENT_DATE - 11, 1),
(2, 55.0, 22.9, CURRENT_DATE - 10, 1),
(3, 56.0, 23.3, CURRENT_DATE - 9, 1),
(4, 57.0, 23.6, CURRENT_DATE - 8, 1),
(5, 58.0, 24.0, CURRENT_DATE - 7, 1),
(6, 59.0, 24.3, CURRENT_DATE - 6, 1),
(7, 60.0, 24.8, CURRENT_DATE - 5, 1),
(8, 61.0, 25.2, CURRENT_DATE - 4, 1),
(9, 62.0, 25.8, CURRENT_DATE - 3, 1),
(10, 63.0, 26.3, CURRENT_DATE - 2, 1),
(11, 64.0, 27.1, CURRENT_DATE - 1, 1),
(12, 66.0, 31.2, CURRENT_DATE, 1),
-- Equateur (ID 2)
(13, 60.0, 24.8, CURRENT_DATE - 11, 2),
(14, 61.0, 25.2, CURRENT_DATE - 10, 2),
(15, 62.0, 25.6, CURRENT_DATE - 9, 2),
(16, 63.0, 26.0, CURRENT_DATE - 8, 2),
(17, 64.0, 26.4, CURRENT_DATE - 7, 2),
(18, 65.0, 26.8, CURRENT_DATE - 6, 2),
(19, 66.0, 27.0, CURRENT_DATE - 5, 2),
(20, 67.0, 27.4, CURRENT_DATE - 4, 2),
(21, 68.0, 27.8, CURRENT_DATE - 3, 2),
(22, 69.0, 28.1, CURRENT_DATE - 2, 2),
(23, 70.0, 28.6, CURRENT_DATE - 1, 2),
(24, 72.0, 30.7, CURRENT_DATE, 2),
-- Colombie (ID 3)
(25, 50.0, 21.8, CURRENT_DATE - 11, 3),
(26, 51.0, 22.1, CURRENT_DATE - 10, 3),
(27, 52.0, 22.4, CURRENT_DATE - 9, 3),
(28, 53.0, 22.8, CURRENT_DATE - 8, 3),
(29, 54.0, 23.1, CURRENT_DATE - 7, 3),
(30, 55.0, 23.4, CURRENT_DATE - 6, 3),
(31, 56.0, 23.7, CURRENT_DATE - 5, 3),
(32, 57.0, 24.0, CURRENT_DATE - 4, 3),
(33, 58.0, 24.4, CURRENT_DATE - 3, 3),
(34, 59.0, 24.8, CURRENT_DATE - 2, 3),
(35, 60.0, 25.3, CURRENT_DATE - 1, 3),
(36, 62.0, 29.4, CURRENT_DATE, 3);

SELECT setval(pg_get_serial_sequence('"capteur"', 'ID_capteur'), 36);