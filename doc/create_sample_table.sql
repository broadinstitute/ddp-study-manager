CREATE TABLE `carrier_service` (
  `carrier_service_id` int(15) NOT NULL AUTO_INCREMENT,
  `carrier` varchar(45) DEFAULT NULL,
  `service` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`carrier_service_id`),
  UNIQUE KEY `carrier_service_id_UNIQUE` (`carrier_service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `kit_dimension` (
  `kit_dimension_id` int(15) NOT NULL AUTO_INCREMENT,
  `kit_width` double NOT NULL,
  `kit_height` double NOT NULL,
  `kit_length` double NOT NULL,
  `kit_weight` double NOT NULL,
  PRIMARY KEY (`kit_dimension_id`),
  UNIQUE KEY `kit_dimension_id_UNIQUE` (`kit_dimension_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `kit_type` (
  `kit_type_id` int(15) NOT NULL AUTO_INCREMENT,
  `kit_type_name` varchar(255) NOT NULL,
  `bsp_material_type` varchar(255) DEFAULT NULL,
  `bsp_receptacle_type` varchar(255) DEFAULT NULL,
  `return_address_name` varchar(255) NOT NULL,
  `return_address_street1` varchar(255) DEFAULT NULL,
  `return_address_street2` varchar(255) DEFAULT NULL,
  `return_address_city` varchar(255) DEFAULT NULL,
  `return_address_state` varchar(45) DEFAULT NULL,
  `return_address_zip` varchar(45) DEFAULT NULL,
  `return_address_country` varchar(45) DEFAULT NULL,
  `return_address_phone` varchar(45) DEFAULT NULL,
  `customs_json` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`kit_type_id`),
  UNIQUE KEY `kit_type_name_UNIQUE` (`kit_type_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_instance` (
  `ddp_instance_id` int(15) NOT NULL AUTO_INCREMENT,
  `instance_name` varchar(255) NOT NULL,
  `base_url` varchar(1000) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '0',
  `bsp_group` varchar(255) DEFAULT NULL,
  `bsp_collection` varchar(255) DEFAULT NULL,
  `bsp_organism` varchar(255) DEFAULT NULL,
  `collaborator_id_prefix` varchar(45) DEFAULT NULL,
  `reminder_notification_wks` int(11) DEFAULT NULL,
  PRIMARY KEY (`ddp_instance_id`),
  UNIQUE KEY `instance_name_UNIQUE` (`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `instance_role` (
  `instance_role_id` int(15) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  PRIMARY KEY (`instance_role_id`),
  UNIQUE KEY `name_UNIQUE` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_instance_role` (
  `ddp_instance_role_id` int(15) NOT NULL AUTO_INCREMENT,
  `ddp_instance_id` int(15) NOT NULL,
  `instance_role_id` int(15) NOT NULL,
  PRIMARY KEY (`ddp_instance_role_id`),
  KEY `ddp_instance_role_fk_idx` (`instance_role_id`),
  KEY `ddp_instance_fk_idx` (`ddp_instance_id`),
  CONSTRAINT `ddp_instance_fk` FOREIGN KEY (`ddp_instance_id`) REFERENCES `ddp_instance` (`ddp_instance_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `ddp_instance_role_fk` FOREIGN KEY (`instance_role_id`) REFERENCES `instance_role` (`instance_role_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_kit_request` (
  `dsm_kit_request_id` int(15) NOT NULL AUTO_INCREMENT,
  `ddp_instance_id` int(15) NOT NULL,
  `ddp_kit_request_id` varchar(45) NOT NULL,
  `kit_type_id` int(15) NOT NULL,
  `bsp_collaborator_participant_id` varchar(200) DEFAULT NULL,
  `bsp_collaborator_sample_id` varchar(200) DEFAULT NULL,
  `ddp_participant_id` varchar(200) NOT NULL,
  `ddp_label` varchar(45) NOT NULL,
  `created_by` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`dsm_kit_request_id`),
  UNIQUE KEY `ddp_label_UNIQUE` (`ddp_label`),
  UNIQUE KEY `ddp_kit_req_instance_uid_idx` (`ddp_kit_request_id`,`ddp_instance_id`),
  KEY `ddp_kit_req_kit_type_fk_idx` (`kit_type_id`),
  KEY `ddp_kit_req_instance_fk_idx` (`ddp_instance_id`),
  CONSTRAINT `ddp_kit_req_instance_fk` FOREIGN KEY (`ddp_instance_id`) REFERENCES `ddp_instance` (`ddp_instance_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `ddp_kit_req_kit_type_fk` FOREIGN KEY (`kit_type_id`) REFERENCES `kit_type` (`kit_type_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_kit` (
  `dsm_kit_id` int(11) NOT NULL AUTO_INCREMENT,
  `dsm_kit_request_id` int(15) NOT NULL,
  `kit_label` varchar(255) DEFAULT NULL,
  `label_url_to` varchar(255) DEFAULT NULL,
  `label_url_return` varchar(255) DEFAULT NULL,
  `easypost_to_id` varchar(45) DEFAULT NULL,
  `easypost_return_id` varchar(45) DEFAULT NULL,
  `easypost_tracking_to_id` varchar(45) DEFAULT NULL,
  `easypost_tracking_return_id` varchar(45) DEFAULT NULL,
  `easypost_tracking_to_url` varchar(255) DEFAULT NULL,
  `easypost_tracking_return_url` varchar(255) DEFAULT NULL,
  `scan_date` bigint(20) DEFAULT NULL,
  `kit_complete` tinyint(1) DEFAULT NULL,
  `error` tinyint(1) DEFAULT NULL,
  `message` varchar(500) DEFAULT NULL,
  `receive_date` bigint(20) DEFAULT NULL,
  `easypost_address_id_to` varchar(45) DEFAULT NULL,
  `deactivated_date` bigint(20) DEFAULT NULL,
  `deactivation_reason` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`dsm_kit_id`),
  UNIQUE KEY `kit_label_UNIQUE` (`kit_label`),
  KEY `ddp_kit_kit_req_fk_idx` (`dsm_kit_request_id`),
  CONSTRAINT `ddp_kit_kit_req_fk` FOREIGN KEY (`dsm_kit_request_id`) REFERENCES `ddp_kit_request` (`dsm_kit_request_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_kit_carrier` (
  `ddp_kit_carrier_id` int(15) NOT NULL AUTO_INCREMENT,
  `ddp_instance_id` int(15) NOT NULL,
  `kit_type_id` int(15) NOT NULL,
  `carrier_service_to_id` int(15) DEFAULT NULL,
  `carrier_service_return_id` int(15) DEFAULT NULL,
  `kit_dimension_id` int(15) NOT NULL,
  PRIMARY KEY (`ddp_kit_carrier_id`),
  UNIQUE KEY `ddp_kit_carrier_id_UNIQUE` (`ddp_kit_carrier_id`),
  KEY `ddp_kit_carrier_service_return_fk_idx` (`carrier_service_return_id`),
  KEY `ddp_kit_carrier_service_to_fk_idx` (`carrier_service_to_id`),
  KEY `ddp_kit_dimension_fk_idx` (`kit_dimension_id`),
  CONSTRAINT `ddp_kit_carrier_service_return_fk` FOREIGN KEY (`carrier_service_return_id`) REFERENCES `carrier_service` (`carrier_service_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `ddp_kit_carrier_service_to_fk` FOREIGN KEY (`carrier_service_to_id`) REFERENCES `carrier_service` (`carrier_service_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `ddp_kit_dimension_fk` FOREIGN KEY (`kit_dimension_id`) REFERENCES `kit_dimension` (`kit_dimension_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT INTO `carrier_service`
(`carrier`,
`service`)
VALUES
('FedEx', 'FEDEX_2_DAY');
INSERT INTO `carrier_service`
(`carrier`,
`service`)
VALUES
('USPS', 'Priority');

INSERT INTO `ddp_instance`
(`instance_name`,
`base_url`,
`is_active`,
`bsp_group`,
`bsp_collection`,
`bsp_organism`)
VALUES
('Angio', 'http://localhost:5555', '0', 'Group Foo', 'SC-123', '1');

INSERT INTO `kit_dimension`
(`kit_width`,
`kit_height`,
`kit_length`,
`kit_weight`)
VALUES
('6.9', '1.3', '5.2', '3.2');

INSERT INTO `kit_dimension`
(`kit_width`,
`kit_height`,
`kit_length`,
`kit_weight`)
VALUES
('4.5', '2', '6.25', '13');


INSERT INTO `kit_type`
(`kit_type_name`,
`bsp_material_type`,
`bsp_receptacle_type`,
`return_address_name`,
`return_address_street1`,
`return_address_street2`,
`return_address_city`,
`return_address_state`,
`return_address_zip`,
`return_address_country`,
`return_address_phone`)
VALUES
('SALIVA', 'Saliva', 'Oragene Kit', 'Broad Institute', 'Attn. Broad Genomics', '320 Charles St - Lab 181', 'Cambridge', 'MA', '02141', 'US', '(617) 714-8952');

INSERT INTO `kit_type`
(`kit_type_name`,
`bsp_material_type`,
`bsp_receptacle_type`,
`return_address_name`,
`return_address_street1`,
`return_address_street2`,
`return_address_city`,
`return_address_state`,
`return_address_zip`,
`return_address_country`,
`return_address_phone`)
VALUES
('BLOOD', 'Whole Blood:Streck Cell-Free Preserved', 'Vacutainer Cell-Free DNA Tube Camo-Top [10mL]', 'Broad Institute', 'Attn. Broad Genomics', '320 Charles St - Lab 181', 'Cambridge', 'MA', '02141', 'US', '(617) 714-8952');

INSERT INTO `ddp_kit_carrier`
(`ddp_instance_id`,
`kit_type_id`,
`carrier_service_to_id`,
`kit_dimension_id`)
VALUES
(1,1,1,1);

INSERT INTO `instance_role`
(`instance_role_id`,
`name`)
VALUES
('1', 'has_kit_request_endpoints');
INSERT INTO `instance_role`
(`instance_role_id`,
`name`)
VALUES
('2', 'kit_request_activated');
INSERT INTO `instance_role`
(`instance_role_id`,
`name`)
VALUES
('3', 'has_medical_record_endpoints');
INSERT INTO `instance_role`
(`instance_role_id`,
`name`)
VALUES
('4', 'medical_record_activated');
INSERT INTO `instance_role`
(`instance_role_id`,
`name`)
VALUES
('5', 'has_mailing_list_endpoint');
INSERT INTO `instance_role`
(`instance_role_id`,
`name`)
VALUES
('6', 'kit_participant_notifications_activated');
INSERT INTO `instance_role`
(`instance_role_id`,
`name`)
VALUES
('7', 'has_exit_participant_endpoint');
