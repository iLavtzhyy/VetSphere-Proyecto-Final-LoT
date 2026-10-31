-- =====================================================================
-- Datos semilla de VetSphere (base SQL Server).
-- IMPORTANTE: este archivo usa sintaxis especifica de SQL Server
-- (SET IDENTITY_INSERT) y passwords BCrypt fijos para la demo.
-- En los perfiles de prueba (H2) NO se ejecuta: spring.sql.init.mode=never
-- y la carga de datos de demostracion la hace VetSphereDataSeeder (JPA).
-- =====================================================================
SET IDENTITY_INSERT rol ON;
INSERT INTO rol (id, nombre) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_VETERINARIO'),
(3, 'ROLE_RECEPCIONISTA'),
(4, 'ROLE_CLIENTE');
SET IDENTITY_INSERT rol OFF;

SET IDENTITY_INSERT usuario ON;
INSERT INTO usuario (id, nombres, apellidos, email, telefono, password, foto_url, especialidad, cedula_profesional, biografia, activo) VALUES
(1,'Valeria','Mendoza','admin1@vetsphere.com','0991000001','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600','Direccion clinica','ADM-001','Administra operaciones, reportes y calidad de servicio.',1),
(2,'Mateo','Rivas','admin2@vetsphere.com','0991000002','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=600','Finanzas y auditoria','ADM-002','Supervisa finanzas, inventario y cumplimiento interno.',1),
(3,'Camila','Ortega','admin3@vetsphere.com','0991000003','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1517841905240-472988babdf9?w=600','Experiencia cliente','ADM-003','Coordina indicadores y protocolos de atencion.',1),
(4,'Santiago','Vera','admin4@vetsphere.com','0991000004','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=600','Tecnologia clinica','ADM-004','Gestiona integraciones digitales de la clinica.',1),
(5,'Daniela','Paz','admin5@vetsphere.com','0991000005','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600','Talento humano','ADM-005','Lidera capacitacion y turnos del equipo.',1),
(6,'Andres','Lopez','admin6@vetsphere.com','0991000006','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600','Operacion hospitalaria','ADM-006','Responsable de hospitalizacion y recursos.',1),
(7,'Renata','Suarez','admin7@vetsphere.com','0991000007','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=600','Compras medicas','ADM-007','Controla compras, proveedores y stock critico.',1),
(8,'Nicolas','Romero','admin8@vetsphere.com','0991000008','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=600','Control interno','ADM-008','Revisa procesos, seguridad y permisos.',1),
(9,'Lucia','Carrion','lucia.carrion@vetsphere.com','0992000001','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=600','Medicina interna','VET-1021','Veterinaria enfocada en diagnostico integral y enfermedades cronicas.',1),
(10,'Emilio','Torres','emilio.torres@vetsphere.com','0992000002','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=600','Cirugia de tejidos blandos','VET-2044','Cirujano con experiencia en procedimientos abdominales y oncologicos.',1),
(11,'Mariana','Cedeno','mariana.cedeno@vetsphere.com','0992000003','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=600','Dermatologia','VET-3310','Especialista en alergias, piel y tratamientos dermatologicos avanzados.',1),
(12,'Pablo','Galarza','pablo.galarza@vetsphere.com','0992000004','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1580489944761-15a19d654956?w=600','Traumatologia','VET-4487','Atiende fracturas, rehabilitacion y ortopedia de pequenas especies.',1),
(13,'Sofia','Navarro','sofia.navarro@vetsphere.com','0992000005','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1607990281513-2c110a25bd8c?w=600','Oftalmologia','VET-5199','Especialista en cornea, retina y cirugia ocular veterinaria.',1),
(14,'Javier','Quintero','javier.quintero@vetsphere.com','0992000006','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=600','Cardiologia','VET-6214','Realiza ecocardiogramas, control cardiaco y medicina preventiva.',1),
(15,'Isabella','Herrera','isabella.herrera@vetsphere.com','0992000007','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1594824476967-48c8b964273f?w=600','Animales exoticos','VET-7802','Manejo clinico de aves, conejos, reptiles y pequenos mamiferos.',1),
(16,'Ana','Molina','recepcion1@vetsphere.com','0993000001','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=600',NULL,NULL,'Recepcionista lider en agenda, admisiones y cobros.',1),
(17,'Luis','Aguilar','recepcion2@vetsphere.com','0993000002','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1527980965255-d3b416303d12?w=600',NULL,NULL,'Gestiona citas, recordatorios y seguimiento de clientes.',1),
(18,'Paula','Castro','recepcion3@vetsphere.com','0993000003','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1554151228-14d9def656e4?w=600',NULL,NULL,'Coordina hospitalizacion y recepcion de emergencias.',1),
(19,'Diego','Benitez','recepcion4@vetsphere.com','0993000004','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1530268729831-4b0b9e170218?w=600',NULL,NULL,'Apoya facturacion, caja y control documental.',1),
(20,'Elena','Vargas','recepcion5@vetsphere.com','0993000005','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=600',NULL,NULL,'Encargada de admision y comunicacion con tutores.',1),
(21,'Rafael','Mejia','recepcion6@vetsphere.com','0993000006','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1507591064344-4c6ce005b128?w=600',NULL,NULL,'Gestiona ventas de farmacia y entrega de indicaciones.',1),
(22,'Gabriela','Salas','recepcion7@vetsphere.com','0993000007','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.','https://images.unsplash.com/photo-1560250097-0b93528c311a?w=600',NULL,NULL,'Atiende canales digitales y confirmacion de citas.',1),
(23,'Martin','Alvarez','cliente1@mail.com','0984000001','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.',NULL,NULL,NULL,'Tutor responsable con dos mascotas activas.',1),
(24,'Carolina','Paredes','cliente2@mail.com','0984000002','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.',NULL,NULL,NULL,'Tutora de paciente geriatra con controles periodicos.',1),
(25,'Julian','Santos','cliente3@mail.com','0984000003','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.',NULL,NULL,NULL,'Tutor con interes en medicina preventiva.',1),
(26,'Laura','Ibarra','cliente4@mail.com','0984000004','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.',NULL,NULL,NULL,'Tutora de felino con historial dermatologico.',1),
(27,'Sebastian','Cruz','cliente5@mail.com','0984000005','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.',NULL,NULL,NULL,'Tutor de paciente internado por monitoreo.',1),
(28,'Fernanda','Luna','cliente6@mail.com','0984000006','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.',NULL,NULL,NULL,'Tutora de mascota exotica.',1),
(29,'Oscar','Reyes','cliente7@mail.com','0984000007','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.',NULL,NULL,NULL,'Tutor de perro deportista.',1),
(30,'Michelle','Flores','cliente8@mail.com','0984000008','$2a$10$r3moefcuT0AE2R/1WF8xcORfy86hIP4fV0KYhRg2IbJxZsHtHJ69.',NULL,NULL,NULL,'Tutora de cachorro en plan vacunal.',1);
SET IDENTITY_INSERT usuario OFF;

INSERT INTO usuario_roles (usuario_id, rol_id) VALUES
(1,1),(2,1),(3,1),(4,1),(5,1),(6,1),(7,1),(8,1),
(9,2),(10,2),(11,2),(12,2),(13,2),(14,2),(15,2),
(16,3),(17,3),(18,3),(19,3),(20,3),(21,3),(22,3),
(23,4),(24,4),(25,4),(26,4),(27,4),(28,4),(29,4),(30,4);

SET IDENTITY_INSERT producto ON;
INSERT INTO producto (id, codigo, nombre, categoria, stock, stock_critico, precio, prioridad_alta) VALUES
(1,'MED-001','Cefalexina 250mg','Antibiotico',11,9,6.25,0),
(2,'MED-002','Meloxicam unidad','Analgesia',18,10,8,0),
(3,'MED-003','Atropina 10ml','Emergencia',25,11,9.75,0),
(4,'MED-004','Solucion Hartmann 250mg','Fluidoterapia',32,12,11.5,1),
(5,'MED-005','Ketamina unidad','Anestesia',39,13,13.25,0),
(6,'MED-006','Vacuna multiple canina 10ml','Vacunas',46,14,15,0),
(7,'MED-007','Vacuna triple felina 250mg','Insumos',53,15,16.75,0),
(8,'MED-008','Omeprazol unidad','Dermatologia',60,16,18.5,1),
(9,'MED-009','Metronidazol 10ml','Antiparasitario',67,17,20.25,0),
(10,'MED-010','Amoxicilina 250mg','Hospitalizacion',74,18,22,0),
(11,'MED-011','Doxiciclina unidad','Antibiotico',81,19,23.75,0),
(12,'MED-012','Prednisolona 10ml','Analgesia',88,20,25.5,1),
(13,'MED-013','Furosemida 250mg','Emergencia',95,21,27.25,0),
(14,'MED-014','Enrofloxacina unidad','Fluidoterapia',7,22,29,0),
(15,'MED-015','Ivermectina 10ml','Anestesia',14,8,30.75,0),
(16,'MED-016','Clorhexidina 250mg','Vacunas',21,9,32.5,1),
(17,'MED-017','Suero fisiologico unidad','Insumos',28,10,34.25,0),
(18,'MED-018','Jeringa 5ml 10ml','Dermatologia',35,11,36,0),
(19,'MED-019','Guantes quirurgicos 250mg','Antiparasitario',42,12,37.75,0),
(20,'MED-020','Collar isabelino unidad','Hospitalizacion',49,13,39.5,1),
(21,'MED-021','Maropitant 10ml','Antibiotico',56,14,41.25,0),
(22,'MED-022','Carprofeno 250mg','Analgesia',63,15,43,0),
(23,'MED-023','Tramadol unidad','Emergencia',70,16,44.75,0),
(24,'MED-024','Diazepam 10ml','Fluidoterapia',77,17,46.5,1),
(25,'MED-025','Propofol 250mg','Anestesia',84,18,48.25,0),
(26,'MED-026','Lidocaina unidad','Vacunas',91,19,50,0),
(27,'MED-027','Gentamicina 10ml','Insumos',98,20,51.75,0),
(28,'MED-028','Ciprofloxacina otica 250mg','Dermatologia',10,21,53.5,1),
(29,'MED-029','Shampoo medicado unidad','Antiparasitario',17,22,55.25,0),
(30,'MED-030','Antipulgas spot on 10ml','Hospitalizacion',24,8,57,0),
(31,'MED-031','Cefalexina 250mg','Antibiotico',31,9,58.75,0),
(32,'MED-032','Meloxicam unidad','Analgesia',38,10,60.5,1),
(33,'MED-033','Atropina 10ml','Emergencia',45,11,62.25,0),
(34,'MED-034','Solucion Hartmann 250mg','Fluidoterapia',52,12,64,0),
(35,'MED-035','Ketamina unidad','Anestesia',59,13,65.75,0),
(36,'MED-036','Vacuna multiple canina 10ml','Vacunas',66,14,67.5,1),
(37,'MED-037','Vacuna triple felina 250mg','Insumos',73,15,69.25,0),
(38,'MED-038','Omeprazol unidad','Dermatologia',80,16,71,0),
(39,'MED-039','Metronidazol 10ml','Antiparasitario',87,17,72.75,0),
(40,'MED-040','Amoxicilina 250mg','Hospitalizacion',94,18,74.5,1),
(41,'MED-041','Doxiciclina unidad','Antibiotico',6,19,76.25,0),
(42,'MED-042','Prednisolona 10ml','Analgesia',13,20,78,0),
(43,'MED-043','Furosemida 250mg','Emergencia',20,21,79.75,0),
(44,'MED-044','Enrofloxacina unidad','Fluidoterapia',27,22,81.5,1),
(45,'MED-045','Ivermectina 10ml','Anestesia',34,8,4.5,0),
(46,'MED-046','Clorhexidina 250mg','Vacunas',41,9,6.25,0),
(47,'MED-047','Suero fisiologico unidad','Insumos',48,10,8,0),
(48,'MED-048','Jeringa 5ml 10ml','Dermatologia',55,11,9.75,1),
(49,'MED-049','Guantes quirurgicos 250mg','Antiparasitario',62,12,11.5,0),
(50,'MED-050','Collar isabelino unidad','Hospitalizacion',69,13,13.25,0),
(51,'MED-051','Maropitant 10ml','Antibiotico',76,14,15,0),
(52,'MED-052','Carprofeno 250mg','Analgesia',83,15,16.75,1),
(53,'MED-053','Tramadol unidad','Emergencia',90,16,18.5,0),
(54,'MED-054','Diazepam 10ml','Fluidoterapia',97,17,20.25,0),
(55,'MED-055','Propofol 250mg','Anestesia',9,18,22,0),
(56,'MED-056','Lidocaina unidad','Vacunas',16,19,23.75,1),
(57,'MED-057','Gentamicina 10ml','Insumos',23,20,25.5,0),
(58,'MED-058','Ciprofloxacina otica 250mg','Dermatologia',30,21,27.25,0),
(59,'MED-059','Shampoo medicado unidad','Antiparasitario',37,22,29,0),
(60,'MED-060','Antipulgas spot on 10ml','Hospitalizacion',44,8,30.75,1),
(61,'MED-061','Cefalexina 250mg','Antibiotico',51,9,32.5,0),
(62,'MED-062','Meloxicam unidad','Analgesia',58,10,34.25,0),
(63,'MED-063','Atropina 10ml','Emergencia',65,11,36,0),
(64,'MED-064','Solucion Hartmann 250mg','Fluidoterapia',72,12,37.75,1),
(65,'MED-065','Ketamina unidad','Anestesia',79,13,39.5,0),
(66,'MED-066','Vacuna multiple canina 10ml','Vacunas',86,14,41.25,0),
(67,'MED-067','Vacuna triple felina 250mg','Insumos',93,15,43,0),
(68,'MED-068','Omeprazol unidad','Dermatologia',5,16,44.75,1),
(69,'MED-069','Metronidazol 10ml','Antiparasitario',12,17,46.5,0),
(70,'MED-070','Amoxicilina 250mg','Hospitalizacion',19,18,48.25,0),
(71,'MED-071','Doxiciclina unidad','Antibiotico',26,19,50,0),
(72,'MED-072','Prednisolona 10ml','Analgesia',33,20,51.75,1),
(73,'MED-073','Furosemida 250mg','Emergencia',40,21,53.5,0),
(74,'MED-074','Enrofloxacina unidad','Fluidoterapia',47,22,55.25,0),
(75,'MED-075','Ivermectina 10ml','Anestesia',54,8,57,0),
(76,'MED-076','Clorhexidina 250mg','Vacunas',61,9,58.75,1),
(77,'MED-077','Suero fisiologico unidad','Insumos',68,10,60.5,0),
(78,'MED-078','Jeringa 5ml 10ml','Dermatologia',75,11,62.25,0),
(79,'MED-079','Guantes quirurgicos 250mg','Antiparasitario',82,12,64,0),
(80,'MED-080','Collar isabelino unidad','Hospitalizacion',89,13,65.75,1),
(81,'MED-081','Maropitant 10ml','Antibiotico',96,14,67.5,0),
(82,'MED-082','Carprofeno 250mg','Analgesia',8,15,69.25,0),
(83,'MED-083','Tramadol unidad','Emergencia',15,16,71,0),
(84,'MED-084','Diazepam 10ml','Fluidoterapia',22,17,72.75,1),
(85,'MED-085','Propofol 250mg','Anestesia',29,18,74.5,0),
(86,'MED-086','Lidocaina unidad','Vacunas',36,19,76.25,0),
(87,'MED-087','Gentamicina 10ml','Insumos',43,20,78,0),
(88,'MED-088','Ciprofloxacina otica 250mg','Dermatologia',50,21,79.75,1),
(89,'MED-089','Shampoo medicado unidad','Antiparasitario',57,22,81.5,0),
(90,'MED-090','Antipulgas spot on 10ml','Hospitalizacion',64,8,4.5,0),
(91,'MED-091','Cefalexina 250mg','Antibiotico',71,9,6.25,0),
(92,'MED-092','Meloxicam unidad','Analgesia',78,10,8,1),
(93,'MED-093','Atropina 10ml','Emergencia',85,11,9.75,0),
(94,'MED-094','Solucion Hartmann 250mg','Fluidoterapia',92,12,11.5,0),
(95,'MED-095','Ketamina unidad','Anestesia',4,13,13.25,0),
(96,'MED-096','Vacuna multiple canina 10ml','Vacunas',11,14,15,1),
(97,'MED-097','Vacuna triple felina 250mg','Insumos',18,15,16.75,0),
(98,'MED-098','Omeprazol unidad','Dermatologia',25,16,18.5,0),
(99,'MED-099','Metronidazol 10ml','Antiparasitario',32,17,20.25,0),
(100,'MED-100','Amoxicilina 250mg','Hospitalizacion',39,18,22,1),
(101,'MED-101','Doxiciclina unidad','Antibiotico',46,19,23.75,0),
(102,'MED-102','Prednisolona 10ml','Analgesia',53,20,25.5,0),
(103,'MED-103','Furosemida 250mg','Emergencia',60,21,27.25,0),
(104,'MED-104','Enrofloxacina unidad','Fluidoterapia',67,22,29,1),
(105,'MED-105','Ivermectina 10ml','Anestesia',74,8,30.75,0),
(106,'MED-106','Clorhexidina 250mg','Vacunas',81,9,32.5,0),
(107,'MED-107','Suero fisiologico unidad','Insumos',88,10,34.25,0),
(108,'MED-108','Jeringa 5ml 10ml','Dermatologia',95,11,36,1),
(109,'MED-109','Guantes quirurgicos 250mg','Antiparasitario',7,12,37.75,0),
(110,'MED-110','Collar isabelino unidad','Hospitalizacion',14,13,39.5,0),
(111,'MED-111','Maropitant 10ml','Antibiotico',21,14,41.25,0),
(112,'MED-112','Carprofeno 250mg','Analgesia',28,15,43,1),
(113,'MED-113','Tramadol unidad','Emergencia',35,16,44.75,0),
(114,'MED-114','Diazepam 10ml','Fluidoterapia',42,17,46.5,0),
(115,'MED-115','Propofol 250mg','Anestesia',49,18,48.25,0),
(116,'MED-116','Lidocaina unidad','Vacunas',56,19,50,1),
(117,'MED-117','Gentamicina 10ml','Insumos',63,20,51.75,0),
(118,'MED-118','Ciprofloxacina otica 250mg','Dermatologia',70,21,53.5,0),
(119,'MED-119','Shampoo medicado unidad','Antiparasitario',77,22,55.25,0),
(120,'MED-120','Antipulgas spot on 10ml','Hospitalizacion',84,8,57,1);
SET IDENTITY_INSERT producto OFF;

SET IDENTITY_INSERT mascota ON;
INSERT INTO mascota (id, nombre, especie, raza, sexo, color, peso_kg, fecha_nacimiento, foto_url, estado, propietario_id) VALUES
(1,'Milo','Canino','Golden Retriever','Macho','Dorado',28.6,'2020-04-12','https://images.unsplash.com/photo-1552053831-71594a27632d?w=900','ACTIVA',23),
(2,'Luna','Felino','Siames','Hembra','Crema',4.2,'2021-09-02','https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=900','ACTIVA',24),
(3,'Rocky','Canino','Bulldog Frances','Macho','Atigrado',12.1,'2019-01-20','https://images.unsplash.com/photo-1581375221876-8f287f9cd2cc?w=900','INTERNADA',27),
(4,'Nala','Felino','Mestizo','Hembra','Negro',3.7,'2022-06-11','https://images.unsplash.com/photo-1574158622682-e40e69881006?w=900','ACTIVA',26),
(5,'Coco','Ave','Ninfa','Macho','Gris',0.09,'2023-02-15','https://images.unsplash.com/photo-1522926193341-e9ffd686c60f?w=900','ACTIVA',28),
(6,'Bruno','Canino','Border Collie','Macho','Blanco y negro',19.5,'2021-12-01','https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=900','ACTIVA',29),
(7,'Kiara','Canino','Labrador','Hembra','Chocolate',24.3,'2024-10-08','https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=900','ACTIVA',30),
(8,'Toby','Canino','Mestizo','Macho','Cafe',10.8,'2018-07-19','https://images.unsplash.com/photo-1517849845537-4d257902454a?w=900','ACTIVA',25);
SET IDENTITY_INSERT mascota OFF;

SET IDENTITY_INSERT historial_clinico ON;
INSERT INTO historial_clinico (id, fecha, tipo_evento, motivo, diagnostico, tratamiento, vacunas, archivos_url, temperatura, frecuencia_cardiaca, frecuencia_respiratoria, mascota_id, veterinario_id) VALUES
(1,'2026-05-01 09:30:00','CONSULTA','Chequeo general','Paciente estable, condicion corporal 4/5. Leve sarro dental.','Profilaxis dental recomendada, control nutricional y antiparasitario mensual.','Rabia vigente',NULL,38.4,92,24,1,9),
(2,'2026-05-16 11:00:00','VACUNA','Plan anual','Sin fiebre ni signos de reaccion previa.','Aplicacion de vacuna multiple y observacion por 30 minutos.','Multiple canina',NULL,38.2,88,22,1,9),
(3,'2026-04-23 10:15:00','CONSULTA','Prurito facial','Dermatitis alergica probable con lesiones leves.','Limpieza local, antihistaminico y dieta hipoalergenica por 21 dias.',NULL,NULL,38.7,130,30,2,11),
(4,'2026-06-04 18:30:00','EMERGENCIA','Vomitos y dolor abdominal','Deshidratacion moderada, sospecha de cuerpo extrano.','Fluidoterapia, analgesia y ecografia abdominal.',NULL,NULL,39.1,118,28,3,10),
(5,'2026-03-18 15:00:00','CONSULTA','Control de peso','Paciente activo, musculo adecuado para actividad deportiva.','Plan de ejercicio progresivo y glucosamina preventiva.',NULL,NULL,38.3,86,20,6,12),
(6,'2026-06-01 12:40:00','CONSULTA','Primer control cachorro','Paciente sano para edad, esquema vacunal incompleto.','Calendario vacunal, desparasitacion y guia nutricional.','Primera dosis multiple',NULL,38.5,110,26,7,9),
(7,'2026-02-10 16:00:00','EXOTICOS','Revision de plumaje','Plumaje irregular por muda y posible deficit nutricional.','Suplementacion, mejora ambiental y control en 30 dias.',NULL,NULL,41.0,220,55,5,15),
(8,'2026-05-28 09:00:00','CONSULTA','Otitis recurrente','Otitis externa leve, sin compromiso timpanico visible.','Limpieza otica, gotas antibioticas y cultivo si recae.',NULL,NULL,38.6,96,24,8,9);
SET IDENTITY_INSERT historial_clinico OFF;

SET IDENTITY_INSERT cirugia ON;
INSERT INTO cirugia (id, fecha_programada, procedimiento, tipo_anestesia, cirujano_principal, constantes_vitales, estado_postoperatorio, estado, notas, mascota_id, cirujano_id) VALUES
(1,'2026-06-05 14:00:00','Extraccion de cuerpo extrano intestinal','General inhalatoria','Dr. Emilio Torres','FC 112, FR 24, SpO2 98%, Temp 38.7','Monitoreo en hospitalizacion, analgesia continua','PROGRAMADA','Requiere ayuno, via permeable y perfil prequirurgico.',3,10),
(2,'2026-06-12 09:30:00','Esterilizacion electiva','General balanceada','Dr. Emilio Torres','FC 104, FR 22, SpO2 99%, Temp 38.4','Recuperacion ambulatoria esperada','PROGRAMADA','Consentimiento informado pendiente.',7,10),
(3,'2026-05-20 10:00:00','Limpieza dental y extraccion simple','General inhalatoria','Dra. Lucia Carrion','FC 90, FR 21, SpO2 99%, Temp 38.2','Alta el mismo dia sin complicaciones','FINALIZADA','Control dental en seis meses.',1,9);
SET IDENTITY_INSERT cirugia OFF;

SET IDENTITY_INSERT factura ON;
INSERT INTO factura (id, numero, fecha, concepto, subtotal, impuestos, total, estado_pago, cliente_id, mascota_id, descuento, cupon_codigo, observaciones) VALUES
(1,'FAC-2026-0001','2026-05-01 10:20:00','Consulta general - Milo',40.00,4.80,44.80,'Pagado',23,1,0.00,NULL,NULL),
(2,'FAC-2026-0002','2026-05-16 11:45:00','Vacunacion multiple - Milo',32.00,3.84,35.84,'Pagado',23,1,0.00,NULL,NULL),
(3,'FAC-2026-0003','2026-04-23 11:10:00','Consulta dermatologica - Luna',65.00,7.80,72.80,'Pendiente',24,2,0.00,NULL,NULL),
(4,'FAC-2026-0004','2026-06-04 19:00:00','Emergencia abdominal - Rocky',110.00,13.20,123.20,'Pagado',27,3,0.00,NULL,NULL),
(5,'FAC-2026-0005','2026-06-05 14:00:00','Cirugia intestinal - Rocky',360.00,43.20,403.20,'Pendiente',27,3,0.00,NULL,NULL),
(6,'FAC-2026-0006','2026-02-10 16:50:00','Consulta exoticos - Coco',55.00,6.60,61.60,'Pagado',28,5,0.00,NULL,NULL),
(7,'FAC-2026-0007','2026-03-18 15:40:00','Control deportivo - Bruno',56.40,6.77,63.17,'Pagado',29,6,1.60,'PROMO-ANALGESIA-10',NULL),
(8,'FAC-2026-0008','2026-06-01 13:10:00','Plan cachorro - Kiara',48.00,5.76,53.76,'Pagado',30,7,0.00,NULL,NULL),
(9,'FAC-2026-0009','2026-05-28 09:45:00','Otitis y farmacia - Toby',72.00,8.64,80.64,'Pendiente',25,8,0.00,NULL,NULL),
(10,'FAC-2026-0010','2026-05-20 12:30:00','Profilaxis dental - Milo',180.00,21.60,201.60,'Pagado',23,1,6.00,NULL,'Incluye descuento permanente del 20% en la vacuna multiple.');
SET IDENTITY_INSERT factura OFF;

-- =====================================================================
-- Detalle de factura: es lo que permite medir la rotacion del inventario
-- (que productos se venden y cuales no). Sin estas filas el analisis de
-- rotacion no puede saber nada.
-- =====================================================================
SET IDENTITY_INSERT factura_detalle ON;
INSERT INTO factura_detalle (id, factura_id, producto_id, descripcion, cantidad, precio_unitario, descuento, subtotal) VALUES
(1,3,NULL,'Consulta dermatologica',1,46.50,0.00,46.50),
(2,3,8,'Omeprazol unidad',1,18.50,0.00,18.50),
(3,7,NULL,'Control deportivo',1,42.00,0.00,42.00),
(4,7,2,'Meloxicam unidad',2,8.00,0.00,16.00),
(5,9,NULL,'Otitis externa - consulta',1,5.00,0.00,5.00),
(6,9,2,'Meloxicam unidad',2,8.00,0.00,16.00),
(7,9,12,'Prednisolona 10ml',2,25.50,0.00,51.00),
(8,10,NULL,'Profilaxis dental',1,120.00,0.00,120.00),
(9,10,6,'Vacuna multiple canina 10ml',2,15.00,6.00,24.00),
(10,10,18,'Jeringa 5ml 10ml',1,36.00,0.00,36.00);
SET IDENTITY_INSERT factura_detalle OFF;

-- =====================================================================
-- Cupones de descuento. Alcance: un producto concreto, una categoria o
-- todo el catalogo (producto_id y categoria a NULL). El descuento se valida
-- y se calcula SIEMPRE en el servidor.
-- =====================================================================
SET IDENTITY_INSERT cupon ON;
INSERT INTO cupon (id, codigo, descripcion, tipo_descuento, valor, producto_id, categoria, minimo_subtotal, fecha_inicio, fecha_fin, usos_maximos, usos_actuales, activo, origen, creado_por, fecha_creacion) VALUES
(1,'PROMO-ANTIPARASITARIO','Promocion de antiflea y antiparasitarios','PORCENTAJE',15.00,NULL,'Antiparasitario',NULL,'2026-01-01 00:00:00','2026-12-31 23:59:59',50,0,1,'MANUAL','admin1@vetsphere.com','2026-01-05 09:00:00'),
(2,'PROMO-ANALGESIA-10','Promocion de analgesia (baja rotacion)','PORCENTAJE',10.00,2,NULL,NULL,'2026-01-10 00:00:00','2026-12-31 23:59:59',30,1,1,'BAJA_ROTACION','admin1@vetsphere.com','2026-01-12 10:30:00'),
(3,'BIENVENIDA5','Descuento de 5 USD en compras superiores a 20 USD','VALOR_FIJO',5.00,NULL,NULL,20.00,'2026-01-01 00:00:00','2026-12-31 23:59:59',100,0,1,'MANUAL','admin2@vetsphere.com','2026-01-03 11:15:00');
SET IDENTITY_INSERT cupon OFF;

-- Trazabilidad de cada canje: que cupon se uso, en que factura y por cuanto.
SET IDENTITY_INSERT canje_cupon ON;
INSERT INTO canje_cupon (id, cupon_id, factura_id, codigo, descuento_aplicado, fecha, usuario_email) VALUES
(1,2,7,'PROMO-ANALGESIA-10',1.60,'2026-03-18 15:40:00','recepcion1@vetsphere.com');
SET IDENTITY_INSERT canje_cupon OFF;

-- =====================================================================
-- Columnas nuevas de producto y usuario (todas opcionales para no romper
-- bases ya creadas con ddl-auto=update).
-- =====================================================================
UPDATE producto SET activo = 1, descuento = 0 WHERE activo IS NULL;
UPDATE usuario SET email_verificado = 1, intentos_fallidos = 0 WHERE email_verificado IS NULL;

-- =====================================================================
-- Modulos que antes devolvian datos inventados y ahora se guardan en la
-- base de datos. Las tablas las crea Hibernate (ddl-auto=update); estas
-- filas son solo datos de referencia para el entorno de demostracion.
-- =====================================================================

-- Turnos del personal: un empleado no puede tener dos franjas solapadas el mismo dia.
SET IDENTITY_INSERT turno ON;
INSERT INTO turno (id, empleado_id, fecha, hora_inicio, hora_fin, area, notas) VALUES
(1, 9,  '2026-09-01','08:00:00','14:00:00','Consulta','Consulta general y controles preventivos'),
(2, 10, '2026-09-01','08:00:00','16:00:00','Cirugia','Bloque quirurgico de la manana'),
(3, 11, '2026-09-01','09:00:00','17:00:00','Laboratorio','Toma de muestras y dermatologia'),
(4, 16, '2026-09-01','07:00:00','15:00:00','Recepcion','Agenda, cobros y entrega de llaves'),
(5, 17, '2026-09-02','07:00:00','15:00:00','Recepcion','Turno de tarde de recepcion');
SET IDENTITY_INSERT turno OFF;

-- Calendario de vacunacion de la clinica.
SET IDENTITY_INSERT vacunacion ON;
INSERT INTO vacunacion (id, mascota_id, vacuna, fecha, fecha_proxima, lote, estado, observaciones) VALUES
(1, 1, 'Vacuna antirrabica','2026-08-10','2027-08-10','RB-2026-014','Aplicada','Sin reacciones adversas observadas'),
(2, 2, 'Triples felina','2026-09-20',NULL,'TR-2026-088','Programada','Se avisa al propietario por correo'),
(3, 3, 'Rabia','2026-05-15','2027-05-15','RB-2025-201','Vencida','Pendiente de reprogramar'),
(4, 8, 'Vacuna multiple canina','2026-09-05',NULL,'MC-2026-045','Programada','Ultima dosis del plan vacunal');
SET IDENTITY_INSERT vacunacion OFF;

-- Seguimientos clinicos (postoperatorios y controles).
SET IDENTITY_INSERT seguimiento ON;
INSERT INTO seguimiento (id, mascota_id, fecha, indicacion, tipo, estado, veterinario_id, observaciones) VALUES
(1, 4, '2026-09-12','Control de suturas a los 10 dias de la cirugia','Postoperatorio','Pendiente',10,'Revisar ausencia de infeccion en la incision'),
(2, 3, '2026-09-25','Control de peso y ajuste de dosis','Control','Pendiente',11,NULL);
SET IDENTITY_INSERT seguimiento OFF;

-- Ingresos por urgencia con su prioridad de atencion.
SET IDENTITY_INSERT urgencia ON;
INSERT INTO urgencia (id, mascota_id, hora_ingreso, prioridad, motivo, estado, veterinario_id, notas) VALUES
(1, 5, '2026-09-10 10:15:00','Alta','Dificultad respiratoria y lengua azulada','En curso',9,'Estabilizacion con oxigeno; se solicita radiografia'),
(2, 6, '2026-09-08 16:40:00','Media','Cojera posterior a salida al parque','Resuelto',11,'Esguince leve; reposo 5 dias y control en 7 dias');
SET IDENTITY_INSERT urgencia OFF;

-- Estudios de imagenologia.
SET IDENTITY_INSERT estudio_imagen ON;
INSERT INTO estudio_imagen (id, mascota_id, tipo, fecha, estado, informe, veterinario_id, archivo_url) VALUES
(1, 5, 'Radiografia','2026-09-10','Informado','Sin alteraciones osteoarticulares relevantes',9,NULL),
(2, 6, 'Ecografia','2026-09-09','Realizado','Ecografia abdominal; informe pendiente de redactar',11,NULL);
SET IDENTITY_INSERT estudio_imagen OFF;

-- Consentimientos firmados por los tutores. El folio lo genera el servidor.
SET IDENTITY_INSERT consentimiento ON;
INSERT INTO consentimiento (id, folio, mascota_id, cirugia_id, tipo, firmado_por, cargo, fecha, estado, documento, observaciones) VALUES
(1, 'CONS-2026-0001', 4, NULL, 'Quirurgico','Martin Alvarez','Propietario','2026-09-01 10:00:00','Firmado','Autorizo la intervencion quirurgica y los controles postoperatorios',NULL),
(2, 'CONS-2026-0002', 3, NULL, 'Vacunacion','Carolina Paredes','Propietaria','2026-08-20 09:30:00','Pendiente',NULL,'Firma pendiente en la proxima visita');
SET IDENTITY_INSERT consentimiento OFF;

-- Proveedores y ordenes de compra (el numero correlativo lo genera el servidor).
SET IDENTITY_INSERT proveedor ON;
INSERT INTO proveedor (id, nombre, contacto, telefono, email, direccion, activo) VALUES
(1, 'Distribuidora Vet Supply','Patricia Gomez','0991122334','ventas@distribuidoravetsupply.com','Av. de las Americas 120',1),
(2, 'Laboratorios Andinos','Diego Fernandez','0985566778','compras@laboratoriosandinos.com','Calle Los Alpes 45',1);
SET IDENTITY_INSERT proveedor OFF;

SET IDENTITY_INSERT orden_compra ON;
INSERT INTO orden_compra (id, numero, proveedor_id, fecha, estado, total, responsable_id, observaciones, detalle) VALUES
(1, 'OC-2026-0001', 1, '2026-09-05','Solicitada',486.90,7,'Entrega prevista en 5 dias habiles','Fresas, suturas y guantes esteriles'),
(2, 'OC-2026-0002', 1, '2026-08-20','Recibida',1240.00,7,'Recibida y verificada por farmacia','Reactivos de laboratorio y jeringas');
SET IDENTITY_INSERT orden_compra OFF;





