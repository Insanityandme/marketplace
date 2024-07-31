<div align="center">
	
# Marketplace - a Blocket clone

[Introduction](#introduction) • [Backend](#backend) • [Frontend](#frontend) • [Screenshots](#screenshots) • [Contact](#contact)
</div>

<div align="center">
  <table>
    <tr>
      <td>
        <img alt="Marketplace search page on desktop" src="https://github.com/user-attachments/assets/3f675dbf-32dc-43eb-92a5-fcd28e9af9d1">
      </td>
      <td>
        <img alt="Marketplace search page on mobile" src="https://github.com/user-attachments/assets/baed83e8-6072-4a38-b135-52d301f0af57">
      </td>
    </tr>
    <tr>
      <td>
        <p>Search page on desktop</p>
      </td>
      <td>
        <p>Search page on mobile</p>
      </td>
    </tr>
  </table>
</div>

# Introduction
Plocket is a web application that allows users to buy and sell a wide range of products. The design of the application is heavily inspired by blocket.se (it’s similar to Facebook's marketplace but for Swedish users). The application is separated into two parts, a frontend and a backend.

Features:

- Login/Register an account
- Sell a product by creating a listing with information & images of a product you want to sell
- Watch current ads the user has published
- Subscribe to categories and get notified via your inbox when a new item in that category is posted
- Add products to your cart
- Buy products (without payment logic implemented)
- Search for products using categories, price, condition, and name
- See order history

# Backend 

Our backend is a web application written in Java using the Spring Boot framework. The backend is a RESTful API which utilizes different HTTP methods for communication. Server sent events are also used, for sending messages from the backend to the frontend. 

This application acts as our web server where we have defined endpoints for our frontend to make use of. 
PostgreSQL is used as the database and digitalocean.com to host content created by users. 
Railway is used to host our database, which provides a containerized environment application.

## Prerequisities 
- Java 21+
- PostgreSQL: [PostgreSQL](https://www.postgresql.org/)
- .env with the following environment variables set:
  - POSTGRES_DB
  - POSTGRES_USER
  - POSTGRES_PASSWORD
  - IMAGE_UPLOAD_DIRECTORY
  - IMAGE_HOST_URL
- (Optional .env variables):
    - MAX_UPLOAD_SIZE_BYTES=10000000
    - SPRING_PROFILES_ACTIVE=production
    - NIXPACKS_JDK_VERSION=21
- Docker for running tests (OPTIONAL): [Docker](https://www.docker.com/)
  
## Installation (TERMINAL)
0. ```git clone https://github.com/Insanityandme/marketplace.git```
1. ```cd backend```

WINDOWS:
1. ```gradlew build -x test```
2. ```gradlew bootRun```

LINUX: 
1. ```./gradlew build -x test```
2. ```./gradlew bootRun```

It should look something like this: ![image](https://github.com/Insanityandme/marketplace-backend/assets/1380257/a8389660-41be-4928-ba69-f040fb17d574)

# Frontend

This is our frontend, a web application written in NextJS. 

## Prequisities

NodeJS: [NodeJS](https://nodejs.org/en)

- (Optional .env variables):
    - NEXT_PUBLIC_BACKEND_URL=http://localhost:8080
    - NEXT_PUBLIC_BACKEND_API_VERSION=v1
    - NEXT_PUBLIC_MAX_FILE_UPLOAD_SIZE_BYTES=10000000

Backend: This will not be functional if you haven't done the instructions for the [backend](#backend)
Marketplace-backend: This will not be functional if you haven't done the instructions for the [backend](https://github.com/Insanityandme/marketplace-backend)

## Installation

0. `git clone https://github.com/Insanityandme/marketplace.git`
1. `cd frontend`
2. `npm install`
3. `npm run dev`
4. Your server should now be running at http://localhost:3000 and should look something like this:

![image](https://github.com/Insanityandme/blocket-clone/assets/1380257/db4d2f92-1072-47bb-a748-d5d6821db985)

---
# Screenshots

---
# Contact
You are welcome to contact me at bengtegardbook@gmail.com if you have any questions on how to setup this environment.
There might be some mistakes in our installation guide and I apologize for that if that's the case.

