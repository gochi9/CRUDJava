# CRUD Java Application (Uni Project)

A Java application for managing multiple types of databases through an intuitive graphical interface.

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Supported Databases](#supported-databases)
- [Getting Started](#getting-started)
    - [Login Screen](#login-screen)
- [Using the Application](#using-the-application)
    - [SQL Databases](#sql-databases)
        - [Table Management](#table-management)
        - [Data Manipulation](#data-manipulation)
        - [Custom Console](#custom-console)
    - [MongoDB](#mongodb)
        - [Data Manipulation](#data-manipulation-1)
        - [Mass Actions](#mass-actions)
- [Notes](#notes)

## Introduction

**CRUD Java** is a Java-based application that provides a platform to interact with various database systems, both SQL and NoSQL. It allows users to perform Create, Read, Update, and Delete (CRUD) operations across different database types through a unified interface.

## Features

- **Multi-Database Support**: Connect to and manage HyperSQL, PostgreSQL, MariaDB, MySQL, SQLite, and MongoDB databases.
- **User Authentication**: Login interfaces tailored for each database type.
- **Table and Collection Management**: Create or delete tables (SQL) and collections (MongoDB) using schemas or manual input.
- **Data Manipulation**: Add, edit, and delete entries, with special considerations for primary keys in SQL databases.
- **Custom Console**: Integrated console for SQL databases with command history, placeholders, and synchronized table views.
- **Pagination and View Control**: Adjust the number of entries per page and navigate through data.
- **Mass Actions**: Perform bulk operations in MongoDB based on specific conditions.
- **Adjustable Interface**: Resize console and input areas, with keyboard shortcuts for enhanced productivity.

## Supported Databases

- **SQL Databases**:
    - HyperSQL
    - PostgreSQL
    - MariaDB
    - MySQL
    - SQLite
- **NoSQL Database**:
    - MongoDB

## Getting Started

### Login Screen

Upon launching the application, you are presented with a selection screen to choose the type of database to connect to. Each database type has its own login interface to capture all necessary connection parameters.

- **Database Selection**: Choose from the supported databases.
- **Credentials Input**: Enter information such as hostname, port, username, and password.
- **Database-Specific Options**: Some databases may require additional settings provided in the login interface.

![Text](https://i.imgur.com/u6VyFjq.png)![Text](https://i.imgur.com/rRzXy8c.png)![Text](https://i.imgur.com/02QY897.png)

---

## Using the Application

The interface varies depending on whether you are working with an SQL database or MongoDB.

### SQL Databases

#### Table Management

After logging in, you will see a list of existing tables in the connected database.

- **Create New Tables**:
    - **Using a Schema**: Import a predefined schema to create a table.
    - **Manual Creation**: Input the table name, define field names, and specify data types.
- **Delete Existing Tables**: Select a table and choose to delete it, with confirmation to prevent accidental deletions.

![Text](https://i.imgur.com/ojdjsuF.png)![Text](https://i.imgur.com/1ZOKf7H.png)![Text](https://i.imgur.com/pvyTOI8.png)![Text](https://i.imgur.com/qBv1hHH.png)![Text](https://i.imgur.com/mWso7Gh.png)

#### Data Manipulation

After selecting a table, you can view and manipulate its data:

- **Add New Fields**: Add new columns to the table.
- **View Entries**:
    - Adjust the number of entries displayed per page.
    - Navigate between pages.
- **Edit Entries**:
    - Modify field values directly in the table view.
    - **Note**: Primary key fields are not editable.
- **Delete Entries**: Select entries to remove them.

#### Custom Console

At the bottom of the SQL database interface is a custom console:

- **Command Execution**: Enter SQL commands.
- **Command History**: Use up (`↑`) and down (`↓`) arrow keys to navigate through previous commands.
- **Placeholders**:
    - Use `{}` as a placeholder for the selected table name.
- **Dynamic Updates**:
    - Executing queries updates the console output and the table view.
- **Adjustable Size**:
    - Resize the console and command input area.
- **Line Breaks**:
    - Use `ALT+ENTER` to insert a line break.

![Text](https://i.imgur.com/UMjyPp1.png)![Text](https://i.imgur.com/ymgZ9hh.png)![Text](https://i.imgur.com/nmGbn8I.png)![Text](https://i.imgur.com/6eiAb3D.png)![Text](https://i.imgur.com/C61VGNU.png)

### MongoDB

#### Data Manipulation

- **Viewing Collections**: All collections are listed upon login.
- **Unified Field Display**:
    - The data grid shows all fields present across documents.
    - Empty cells represent fields not present in a document.
    - `'-'` indicates fields that exist but have empty values.
- **Adding Entries**:
    - Insert new documents.
    - **Field Management**:
        - Create new fields as needed.
        - Load field models from existing documents.
- **Editing Entries**: Modify any field directly in the data grid.
- **Removing Entries**: Select documents to delete them.

#### Mass Actions

The Mass Action feature allows bulk operations:

- **Add Multiple Entries**: Insert several documents at once.
- **Add/Remove Fields**: Modify multiple documents' structure.
- **Conditional Operations**: Apply actions based on conditions.
- **Batch Deletions**: Remove documents that meet specified conditions.

![Text](https://i.imgur.com/92DDqwJ.png)![Text](https://i.imgur.com/caJhKjd.png)![Text](https://i.imgur.com/LZIsB1b.png)![Text](https://i.imgur.com/ZGfr569.png)![Text](https://i.imgur.com/a15eIz0.png)![Text](https://i.imgur.com/kBuIWSG.png)![Text](https://i.imgur.com/H3B76YT.png)![Text](https://i.imgur.com/1UoFhUk.png)![Text](https://i.imgur.com/blJgiwC.png)

---

## Notes

- Modifying the primary key of an SQL table entry is restricted to maintain data integrity.
- In the custom console, `{}` can be used as a placeholder for the current table name. For example, `SELECT * FROM {}`.
- To insert a line break in the console, press `ALT+ENTER` while typing your command.

---
