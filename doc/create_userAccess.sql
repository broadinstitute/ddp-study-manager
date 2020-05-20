CREATE TABLE `access_role` (
  `role_id` int(15) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `name_UNIQUE` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `access_user` (
  `user_id` int(15) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  `email` varchar(45) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email_UNIQUE` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_group` (
  `group_id` int(15) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`group_id`),
  UNIQUE KEY `name_UNIQUE` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ddp_instance_group` (
  `instance_group_id` int(15) NOT NULL AUTO_INCREMENT,
  `ddp_instance_id` int(15) NOT NULL,
  `ddp_group_id` int(15) NOT NULL,
  PRIMARY KEY (`instance_group_id`),
  KEY `instance_fk_idx` (`ddp_instance_id`),
  KEY `instance_group_fk_idx` (`ddp_group_id`),
  CONSTRAINT `ddp_instance_group_fk` FOREIGN KEY (`ddp_group_id`) REFERENCES `ddp_group` (`group_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `instance_group_instance_fk` FOREIGN KEY (`ddp_instance_id`) REFERENCES `ddp_instance` (`ddp_instance_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `access_user_role_group` (
  `user_role_group_id` int(15) NOT NULL AUTO_INCREMENT,
  `user_id` int(15) NOT NULL,
  `role_id` int(15) NOT NULL,
  `group_id` int(15) NOT NULL,
  PRIMARY KEY (`user_role_group_id`),
  KEY `user_role_role_fk_idx` (`role_id`),
  KEY `user_role_group_fk_idx` (`group_id`),
  KEY `user_role_user_fk_idx` (`user_id`),
  CONSTRAINT `user_role_grup_fk` FOREIGN KEY (`group_id`) REFERENCES `ddp_group` (`group_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `user_role_role_fk` FOREIGN KEY (`role_id`) REFERENCES `access_role` (`role_id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `user_role_user_fk` FOREIGN KEY (`user_id`) REFERENCES `access_user` (`user_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT INTO `access_role`
(`role_id`,
`name`)
VALUES
('1', 'kit_shipping');
INSERT INTO `access_role`
(`role_id`,
`name`)
VALUES
('2', 'mr_request');
INSERT INTO `access_role`
(`role_id`,
`name`)
VALUES
('3', 'mr_view');
INSERT INTO `access_role`
(`role_id`,
`name`)
VALUES
('4', 'mailingList_view');
INSERT INTO `access_role`
(`role_id`,
`name`)
VALUES
('5', 'kit_upload');
INSERT INTO `access_role`
(`role_id`,
`name`)
VALUES
('6', 'kit_deactivation');
INSERT INTO `access_role`
(`role_id`,
`name`)
VALUES
('7', 'participant_exit');


INSERT INTO `ddp_group`
(`group_id`,
`name`,
`description`)
VALUES
('1', 'cmi', 'Count Me In');


INSERT INTO `ddp_instance_group`
(`instance_group_id`,
`ddp_instance_id`,
`ddp_group_id`)
VALUES
('1', '1', '1');


INSERT INTO `access_user`
(`user_id`,
`name`,
`email`,
`is_active`)
VALUES
('1', 'Mary McGillicuddy', 'mmcgilli@broadinstitute.org', '1');

INSERT INTO `access_user`
(`user_id`,
`name`,
`email`,
`is_active`)
VALUES
('2', 'Mike Dunphy', 'mdunphy@broadinstitute.org', '1');

INSERT INTO `access_user`
(`user_id`,
`name`,
`email`,
`is_active`)
VALUES
('3', 'Elana Anastasio', 'elana@broadinstitute.org', '1');

INSERT INTO `access_user`
(`user_id`,
`name`,
`email`,
`is_active`)
VALUES
('4', 'Katie Larkin', 'klarkin@broadinstitute.org', '1');

INSERT INTO `access_user`
(`user_id`,
`name`,
`email`,
`is_active`)
VALUES
('5', 'Sam Pollock', 'spollock@broadinstitute.org', '1');

INSERT INTO `access_user`
(`user_id`,
`name`,
`email`,
`is_active`)
VALUES
('6', 'Emily Moore', 'emoore@broadinstitute.org', '1');

INSERT INTO `access_user`
(`user_id`,
`name`,
`email`,
`is_active`)
VALUES
('7', 'Simone Maiwald', 'simone@broadinstitute.org', '1');

INSERT INTO `access_user`
(`user_id`,
`name`,
`email`,
`is_active`)
VALUES
('8', 'Esme Baker', 'ebaker@broadinstitute.org', '1');

INSERT INTO `access_user`
(`user_id`,
`name`,
`email`,
`is_active`)
VALUES
('9', 'Andrew Zimmer', 'andrew@broadinstitute.org', '1');

#Mary
INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('1', '1', '1', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('2', '1', '2', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('3', '1', '4', '1');

#Mike
INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('4', '2', '1', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('5', '2', '2', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('6', '2', '3', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('7', '2', '4', '1');

#Elana
INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('8', '3', '1', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('9', '3', '2', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('10', '3', '3', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('11', '3', '4', '1');

#Katie
INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('12', '4', '1', '1');

#Sam
INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('13', '5', '1', '1');

#Emily
INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('14', '6', '1', '1');

#Simone
INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('15', '7', '1', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('16', '7', '3', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('17', '7', '4', '1');

#Esme
INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('18', '8', '1', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('19', '8', '3', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('20', '8', '4', '1');

#Zim
INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('21', '9', '1', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('22', '9', '3', '1');

INSERT INTO `access_user_role_group`
(`user_role_group_id`,
`user_id`,
`role_id`,
`group_id`)
VALUES
('23', '9', '4', '1');


ALTER TABLE `ddp_participant`
ADD INDEX `participant_user_fk_idx` (`assignee_id` ASC);
ALTER TABLE `ddp_participant`
ADD CONSTRAINT `participant_user_fk`
  FOREIGN KEY (`assignee_id`)
  REFERENCES `access_user` (`user_id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;