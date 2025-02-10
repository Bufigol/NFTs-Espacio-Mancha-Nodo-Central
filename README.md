# NFTs Espacio Mancha - Nodo Central

Sistema de gestión de NFTs para la galería de arte Espacio Mancha con blockchain e IPFS privados.

## Características

- Blockchain privada para registro de NFTs y transacciones
- Sistema IPFS privado para almacenamiento de obras
- Nodo central para coordinación de la red
- Gestión de artistas y obras
- Registro completo de propiedad y ventas
- Sistema de carteras digitales

## Requisitos

- Java 17 o superior
- Maven 3.8.x
- Base de datos (pendiente de especificar)
- Mínimo 8GB RAM
- 50GB espacio en disco

## Instalación

1. Clonar el repositorio
```bash
git clone [url-repositorio]
cd NFTs-Espacio-Mancha-Nodo-Central
```

2. Compilar el proyecto
```bash
mvn clean install
```

3. Configurar el archivo `application.properties`
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

4. Ejecutar el nodo central
```bash
mvn javafx:run
```

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/es/espaciomancha/
│   │   ├── blockchain/    # Implementación de blockchain
│   │   ├── ipfs/         # Sistema de almacenamiento
│   │   ├── network/      # Gestión de red
│   │   ├── model/        # Entidades del dominio
│   │   └── ui/           # Interfaz de usuario
│   └── resources/        # Configuraciones
└── test/                 # Tests unitarios
```

## Arquitectura

- **Blockchain**: Registro inmutable de transacciones y propiedad
- **IPFS**: Almacenamiento distribuido de obras
- **Red P2P**: Comunicación entre nodos
- **API REST**: Interfaz para clientes externos

## Licencia

[Pendiente de definir]

## Equipo

- [Pendiente de completar]