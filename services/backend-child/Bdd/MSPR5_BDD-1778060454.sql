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
	"ID_expedition" integer,
	"date_peremption" integer NOT NULL,
	"qualité" varchar(255) NOT NULL,
	"quantité" integer NOT NULL,
	PRIMARY KEY ("ID_LOT")
);

CREATE TABLE IF NOT EXISTS "chargement" (
	"ID_chargement" serial NOT NULL,
	"datetime" date,
	PRIMARY KEY ("ID_chargement")
);

CREATE TABLE IF NOT EXISTS "expedition" (
	"ID_expedition" serial NOT NULL,
	"datetime" date,
	PRIMARY KEY ("ID_expedition")
);

CREATE TABLE IF NOT EXISTS "capteur" (
	"ID_capteur" serial NOT NULL,
	"humidité" double precision,
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

ALTER TABLE "lot"
ADD CONSTRAINT "lot_fk4"
FOREIGN KEY ("ID_expedition")
REFERENCES "expedition"("ID_expedition");

ALTER TABLE "capteur"
ADD CONSTRAINT "capteur_fk4"
FOREIGN KEY ("ID_entrepot")
REFERENCES "entrepot"("ID_ENTREPOT");