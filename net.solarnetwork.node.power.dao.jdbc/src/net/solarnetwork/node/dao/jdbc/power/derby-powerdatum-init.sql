CREATE TABLE solarnode.sn_power_datum (
	id				BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY,
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	source_id 		VARCHAR(255),
	price_loc_id	BIGINT,
	watts 			INTEGER,
	watt_hours		BIGINT,
	bat_volts		DOUBLE,
	bat_amp_hrs		DOUBLE,
	dc_out_volts	DOUBLE,
	dc_out_amps		DOUBLE,
	ac_out_volts	DOUBLE,
	ac_out_amps		DOUBLE,
	amp_hours		DOUBLE,
	PRIMARY KEY (id)
);

CREATE INDEX power_datum_created_idx ON solarnode.sn_power_datum (created);

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_power_datum.version', '10');

CREATE TABLE solarnode.sn_power_datum_upload (
	power_datum_id	BIGINT NOT NULL,
	destination		VARCHAR(255) NOT NULL,
	created			TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP,
	track_id		BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY,
	PRIMARY KEY (power_datum_id, destination)
);

ALTER TABLE solarnode.sn_power_datum_upload ADD CONSTRAINT
sn_power_datum_upload_power_datum_fk FOREIGN KEY (power_datum_id)
REFERENCES solarnode.sn_power_datum ON DELETE CASCADE;

INSERT INTO solarnode.sn_settings (skey, svalue) 
VALUES ('solarnode.sn_power_datum_upload.version', '1');
