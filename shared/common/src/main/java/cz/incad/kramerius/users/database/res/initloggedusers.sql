-- sekvence pro aktivni uzivatele --
CREATE SEQUENCE ACTIVE_USERS_ID_SEQUENCE INCREMENT BY 1 START WITH 1 MINVALUE 0;

-- sekvence pro mapovani --
CREATE SEQUENCE ACTIVE_USERS_ID_SEQUENCE_2_ROLES_ID_SEQUENCE INCREMENT BY 1 START WITH 1 MINVALUE 0;

-- sekvence pro session keys --
CREATE SEQUENCE SESSION_KEYS_ID_SEQUENCE INCREMENT BY 1 START WITH 1 MINVALUE 0;


-- tabulka aktivnich uzivatelu (databazovych, shibboletich,..)
CREATE TABLE ACTIVE_USERS(ACTIVE_USERS_ID INT NOT NULL, 
        LOGINNAME VARCHAR(255),
        FIRSTNAME VARCHAR (255),
        SURNAME VARCHAR(255),
        PRIMARY KEY (ACTIVE_USERS_ID));


-- mapovani aktivnich uzivatelu na role --
CREATE TABLE ACTIVE_USERS_2_ROLES (
	ACTIVE_USERS_2_ROLES_ID INT NOT NULL,
	ACTIVE_USERS_ID INT REFERENCES ACTIVE_USERS(ACTIVE_USERS_ID), 
	ROLE_ID INT REFERENCES GROUP_ENTITY (GROUP_ID),
	PRIMARY KEY(ACTIVE_USERS_2_ROLES_ID));

-- tabulka pro klice v http session -- 
CREATE TABLE SESSION_KEYS(SESSION_KEYS_ID INT NOT NULL, 
        SESSION_KEY VARCHAR(255),
	LOGGED TIMESTAMP,
	REMOTE_ADDRESS VARCHAR(255),
	ACTIVE VARCHAR(1),
        ACTIVE_USERS_ID INT REFERENCES ACTIVE_USERS(ACTIVE_USERS_ID),
	PRIMARY KEY (SESSION_KEYS_ID));
        

