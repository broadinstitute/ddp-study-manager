CREATE TABLE `ddp_participant` (
  `participant_id` int(11) NOT NULL AUTO_INCREMENT,
  `ddp_participant_id` varchar(200) NOT NULL,
  `last_version` int(15) NOT NULL,
  `last_version_date` varchar(45) NOT NULL,
  `ddp_instance_id` int(15) NOT NULL,
  `release_completed` tinyint(1) DEFAULT NULL,
  `assignee_id` int(11) DEFAULT NULL,
  `last_changed` bigint(20) NOT NULL,
  PRIMARY KEY (`participant_id`),
  UNIQUE KEY `ddp_participant_id_UNIQUE` (`ddp_participant_id`),
  KEY `fk_participant_ddp_instance1_idx` (`ddp_instance_id`),
  CONSTRAINT `fk_participant_ddp_instance1` FOREIGN KEY (`ddp_instance_id`) REFERENCES `ddp_instance` (`ddp_instance_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_institution` (
  `institution_id` int(11) NOT NULL AUTO_INCREMENT,
  `ddp_institution_id` varchar(45) NOT NULL,
  `type` varchar(45) NOT NULL,
  `participant_id` int(11) NOT NULL,
  `last_changed` bigint(20) NOT NULL,
  PRIMARY KEY (`institution_id`),
  UNIQUE KEY `fk_institution_uq` (`ddp_institution_id`,`type`,`participant_id`),
  KEY `fk_participant_idx` (`participant_id`),
  CONSTRAINT `fk_participant` FOREIGN KEY (`participant_id`) REFERENCES `ddp_participant` (`participant_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_medical_record` (
  `medical_record_id` int(11) NOT NULL AUTO_INCREMENT,
  `institution_id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `contact` varchar(255) DEFAULT NULL,
  `phone` varchar(45) DEFAULT NULL,
  `fax` varchar(45) DEFAULT NULL,
  `fax_sent` varchar(45) DEFAULT NULL,
  `fax_confirmed` varchar(45) DEFAULT NULL,
  `mr_received` varchar(45) DEFAULT NULL,
  `mr_document` varchar(45) DEFAULT NULL,
  `mr_problem` tinyint(1) DEFAULT NULL,
  `mr_problem_text` varchar(1000) DEFAULT NULL,
  `unable_obtain` tinyint(1) DEFAULT NULL,
  `duplicate` tinyint(1) DEFAULT NULL,
  `international` tinyint(1) DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `last_changed` bigint(20) NOT NULL,
  `changed_by` varchar(45) NOT NULL,
  PRIMARY KEY (`medical_record_id`),
  KEY `fk_institution_idx` (`institution_id`),
  CONSTRAINT `fk_institution` FOREIGN KEY (`institution_id`) REFERENCES `ddp_institution` (`institution_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_medical_record_log` (
  `medical_record_log_id` int(11) NOT NULL AUTO_INCREMENT,
  `medical_record_id` int(11) NOT NULL,
  `date` varchar(45) DEFAULT NULL,
  `comments` varchar(1000) DEFAULT NULL,
  `type` varchar(45) NOT NULL,
  `last_changed` bigint(20) NOT NULL,
  PRIMARY KEY (`medical_record_log_id`),
  KEY `fk_medical_record_idx` (`medical_record_id`),
  CONSTRAINT `fk_medical_record` FOREIGN KEY (`medical_record_id`) REFERENCES `ddp_medical_record` (`medical_record_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_onc_history` (
  `onc_history_id` int(11) NOT NULL AUTO_INCREMENT,
  `participant_id` int(11) NOT NULL,
  `created` varchar(45) DEFAULT NULL,
  `reviewed` varchar(45) DEFAULT NULL,
  `last_changed` bigint(20) NOT NULL,
  PRIMARY KEY (`onc_history_id`),
  UNIQUE KEY `participant_id_UNIQUE` (`participant_id`),
  KEY `fk_onc_his_participant1_idx` (`participant_id`),
  CONSTRAINT `fk_onc_his_participant1` FOREIGN KEY (`participant_id`) REFERENCES `ddp_participant` (`participant_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `bookmark` (
  `bookmark_id` int(15) NOT NULL AUTO_INCREMENT,
  `value` int(15) NOT NULL,
  `instance` varchar(10) NOT NULL,
  PRIMARY KEY (`bookmark_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EMAIL_QUEUE` (
  `EMAIL_ID` int(11) NOT NULL AUTO_INCREMENT,
  `EMAIL_DATE_CREATED` bigint(20) NOT NULL,
  `EMAIL_DATE_SCHEDULED`  bigint(20) NOT NULL,
  `EMAIL_DATE_PROCESSED` bigint(20),
  `REMINDER_TYPE` varchar(50) NOT NULL,
  `EMAIL_RECORD_ID` varchar(200) NOT NULL,
  `EMAIL_TEMPLATE` varchar(50) NOT NULL,
  `EMAIL_DATA` varchar(2000) NOT NULL,
  PRIMARY KEY (`EMAIL_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_participant_exit` (
  `ddp_participant_exit_id` int(15) NOT NULL AUTO_INCREMENT,
  `ddp_instance_id` int(15) NOT NULL,
  `ddp_participant_id` varchar(200) NOT NULL,
  `exit_date` bigint(20) NOT NULL,
  `exit_by` int(11) NOT NULL,
  PRIMARY KEY (`ddp_participant_exit_id`),
  KEY `participant_exit_user_fk_idx` (`exit_by`),
  KEY `participant_exit_instance_fk_idx` (`ddp_instance_id`),
  CONSTRAINT `participant_exit_instance_fk` FOREIGN KEY (`ddp_instance_id`) REFERENCES `ddp_instance` (`ddp_instance_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `participant_exit_user_fk` FOREIGN KEY (`exit_by`) REFERENCES `access_user` (`user_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EVENT_QUEUE` (
  `EVENT_ID` int(11) NOT NULL AUTO_INCREMENT,
  `EVENT_DATE_CREATED` bigint(20) NOT NULL,
  `EVENT_TYPE` varchar(50) NOT NULL,
  `DDP_INSTANCE_ID` int(15) NOT NULL,
  `DSM_KIT_REQUEST_ID` int(15) NOT NULL,
  PRIMARY KEY (`EVENT_ID`),
  KEY `FK_DDP_INSTANCE_ID_idx` (`DDP_INSTANCE_ID`),
  CONSTRAINT `FK_DDP_INSTANCE_ID` FOREIGN KEY (`DDP_INSTANCE_ID`) REFERENCES `ddp_instance` (`ddp_instance_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


#//Only to help testing! not needed to setup new database! delete!!!

INSERT INTO `bookmark`
(`value`,`instance`)
VALUES
(0,1);
