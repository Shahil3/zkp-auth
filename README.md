# zkp-auth


/zkp-auth/
│
├── /client/
│   ├── /src/
│   │   ├── /main/
│   │   │   ├── /java/
│   │   │   │   ├── com/zkp/client/
│   │   │   │   │   ├── KeyManager.java       # Handles key generation, storage, retrieval
│   │   │   │   │   ├── AuthClient.java        # Signup, login logic (calls server)
│   │   │   │   │   ├── CryptoUtils.java       # Sign, hash, random utilities
│   │   │   │   │   ├── NetworkClient.java     # API communication with server (HTTP helper)
│   │   │   │   │   └── Exceptions.java        # Custom exceptions (optional)
│   │   └── /test/
│   │       └── /java/com/zkp/client/           # Unit tests
│
├── /server/
│   ├── /src/
│   │   ├── /main/
│   │   │   ├── /java/
│   │   │   │   ├── com/zkp/server/
│   │   │   │   │   ├── AuthServer.java        # Signup, challenge generation, verification logic
│   │   │   │   │   ├── UserManager.java       # User + public key database
│   │   │   │   │   ├── ChallengeManager.java  # Challenge lifecycle manager
│   │   │   │   │   ├── CryptoUtils.java       # Signature verification utilities
│   │   │   │   │   └── Exceptions.java        # Custom exceptions (optional)
│   │   └── /test/
│   │       └── /java/com/zkp/server/           # Unit tests
│
├── README.md
├── LICENSE
└── build.gradle (or pom.xml if Maven)