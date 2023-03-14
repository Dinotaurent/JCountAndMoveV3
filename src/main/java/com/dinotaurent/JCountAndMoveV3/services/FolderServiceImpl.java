package com.dinotaurent.JCountAndMoveV3.services;

import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author dandazme
 */
@Service
public class FolderServiceImpl implements IFolderService {

    //Variables y constantes
    private static final Logger LOG = Logger.getLogger(FolderServiceImpl.class);
    private static final String PATH_ENTRADA = "C:\\Users\\la-pu\\Desktop\\PruebasServicios\\Rutas";
    private static final String PATH_TEMP = "C:\\Users\\la-pu\\Desktop\\PruebasServicios\\Rutas\\temp\\";
    private int contadorEntrada;
    private int contadorTemp;
    private File pathEntrada = new File(PATH_ENTRADA);
    private File pathTemp = new File(PATH_TEMP);
    private boolean rezagado = false;
    private int cantidadAnterior = 0;
    private int cantidadAnterior2 = 0;
    private int contador;
    private List<String> nombresArchivosEntrada = new ArrayList<>();
    private List<String> nombresArchivosTemporal = new ArrayList<>();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("MMddyyyyHHmmss");
    private Path mover;
    private boolean renombrado = false;
    private int MAX_INTENTOS = 3;
    private int intentos = 0;
    private int contador2 = 0;
    private boolean nuevos = false;

    //Filtra los documentos de la ruta de entrada que no esten ocultos, terminen en dicha extension y que tengan peso.
    FileFilter filtro = (File file) -> !file.isHidden() && file.getName().endsWith(".txt") && file.length() > 0;

    /* Secuencias y escenarios
    1. Secuencia A: Cantidad de documentos mayor a la admitida o bloqueo de los servicios, documentos nuevos que superan el limite.  linea: 170.
    2. Secuencia B: Se dosifican documentos a la ruta de entrada.  linea: 207.
    */

    //Se proporciona el intervalo de ejecucion.
    @Scheduled(fixedRate = 5000)
    @Override
    public void contar() {
        System.out.println("Se ejecuta metodo contar");
        //Se obtiene la cantidad de documentos en las rutas.
        try {
            contadorEntrada = Objects.requireNonNull(pathEntrada.listFiles(filtro)).length;
            contadorTemp = Objects.requireNonNull(pathTemp.listFiles(filtro)).length;
        } catch (Exception e) {
            LOG.error("Error al contar los documentos: " + e);
        }

        //Se valida los contadores y se ejecuta acciones dependiendo de su valor.
        if (contadorEntrada == 0 && contadorTemp == 0) {
            LOG.info("No se encontraron documentos pendientes");
        } else if (contadorEntrada > 0) {
            LOG.info("Se encontraron: " + contadorEntrada + " documentos en la ruta de entrada.");
            if (contadorEntrada > 5 && contadorTemp > 0) {
                LOG.info("Se detectaron documentos nuevos que bloquearian el servicio, se procedera moverlos a la temporal.");
                obtenerNombresArchivos();
                nuevos = true;
            } else if (contadorEntrada > 5) {
                LOG.info("La cantidad de documentos supera el limite permitido en la ruta de entrada lo cual bloqueara los servicio, se procedera mover los documentos y reiniciar servicios.");
                obtenerNombresArchivos();
            } else {
                //Se valida que no existan rezagos en la ruta de entrada.
                if (contador < 3) {
                    if (contador == 0 || contadorEntrada == cantidadAnterior || contadorEntrada == cantidadAnterior2) {
                        contador++;
                    } else {
                        contador = 0;
                    }
                } else if (!rezagado) {
                    LOG.info("No se detecto movimiento durante mucho tiempo, se procedera a mover los documentos a la temporal y reiniciar los servicios.");
                    rezagado = true;
                    obtenerNombresArchivos();
                }
                cantidadAnterior2 = cantidadAnterior;
                cantidadAnterior = contadorEntrada;
            }
        } else if (contadorEntrada == 0 && contadorTemp > 0) {
            obtenerNombresArchivos();
        }

    }

    @Override
    public void obtenerNombresArchivos() {
        System.out.println("Se ejecuta metodo obtenerNombreArchivos");
        // Se almacena los nombres de los archivos de ambas carpetas.
        File[] archivosEntrada = pathEntrada.listFiles(filtro);
        File[] archivosTemporal = pathTemp.listFiles(filtro);

        // Se valida nuevamente los contadores para evitar iteraciones innecesarias.
        // Con el operador map se agrega la fecha de creacion al nombre y se ordena descendientemente y luego es retirada.
        if (contadorEntrada > 0 || nuevos) {
            nombresArchivosEntrada = Arrays.stream(archivosEntrada)
                    .map(file -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                            FileTime time = attrs.creationTime();
                            return SDF.format(new Date(time.toMillis())) + file.getName();
                        } catch (IOException e) {
                            LOG.error(e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted()
                    .map(archivo -> {
                        StringBuilder sb = new StringBuilder(archivo);
                        for (int i = 0; i < 14; i++) {
                            sb.deleteCharAt(0);
                        }
                        return String.valueOf(sb);
                    })
                    .collect(Collectors.toList());
            if (nuevos){
                nombresArchivosEntrada.subList(0,5).clear();
            }
        }

        if (contadorTemp > 0 && !rezagado && !nuevos) {
            nombresArchivosTemporal = Arrays.stream(archivosTemporal)
                    .map(file -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                            FileTime time = attrs.creationTime();
                            return SDF.format(new Date(time.toMillis())) + file.getName();
                        } catch (IOException e) {
                            LOG.error(e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted()
                    .map(archivo -> {
                        StringBuilder sb = new StringBuilder(archivo);
                        for (int i = 0; i < 14; i++) {
                            sb.deleteCharAt(0);
                        }
                        return String.valueOf(sb);
                    })
                    .collect(Collectors.toList());
        }
        mover();
    }

    @Override
    public void mover() {
        //Se mueven documentos dependiendo de los contadores.

        //Secuencia: A
        if (contadorEntrada > 5 && contadorTemp == 0 || rezagado || nuevos) {
            System.out.println("Entra en la secuencia A");
            //Se recorre la lista de documentos y se crea un path
            nombresArchivosEntrada.forEach(archivo -> {
                Path documento = Paths.get(PATH_ENTRADA).resolve(archivo);
                Path destino = Paths.get(PATH_TEMP).resolve(archivo);

                //Se intenta mover el documento.
                try {
                    mover = Files.move(documento, destino.resolveSibling(destino));
                    LOG.info("Se movio el archivo " + documento + " a la ruta: " + PATH_TEMP);
                } catch (IOException e) {
                    LOG.error(e);
                    do {
                        try {
                            Files.move(documento, destino.resolveSibling(destino));
                            renombrado = true;
                        } catch (IOException ex) {
                            if (++intentos >= MAX_INTENTOS) {
                                LOG.error("No se pudo mover el archivo después de " + MAX_INTENTOS + " intentos", ex);
                            }
                            LOG.info("El archivo no se puede mover debido a que esta en uso, se volverá a intentar en 5 segundos");
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException exx) {
                                LOG.error(exx);
                            }
                        }
                    } while (!renombrado);
                }
            });
            limpiar();
            reiniciarServicios();
            contar();
        }

        //Secuencia: B
        if (contadorEntrada == 0 && contadorTemp > 0 && !rezagado) {
            System.out.println("Entra en la secuenca B");
            LOG.info("Se procedera a dosificar documentos a la entrada.");
            nombresArchivosTemporal.forEach(archivo -> {
                if (contador2 < 5) {
                    Path documento = Paths.get(PATH_TEMP).resolve(archivo);
                    Path destino = Paths.get(PATH_ENTRADA).resolve(archivo);

                    //Se intenta mover el documento.
                    try {
                        Files.move(documento, destino.resolveSibling(destino));
                        LOG.info("Se movio el archivo " + documento + " a la ruta: " + PATH_ENTRADA);
                        contador2++;
                    } catch (Exception e) {
                        LOG.error(e);
                    }
                }
            });
            limpiar();
        }
    }

    @Override
    public void limpiar() {
        System.out.println("Se ejecuta metodo limpiarListas");
        contador2 = 0;
        renombrado = false;
        contador = 0;
        rezagado = false;
        nombresArchivosEntrada.clear();
        nombresArchivosTemporal.clear();
        contadorEntrada = 0;
        contadorTemp = 0;
        nuevos = false;
    }

    @Override
    public void reiniciarServicios() {
        System.out.println("Se ejecuta metodo reiniciarServicios");

        //Se intenta reinciar los servicios.
        try {
            String[] cmd = {
                    "sc.exe stop JServicioTestV3",
                    "sc.exe config \"JServicioTestV3\" obj= \".\\usuario3\" password= \"admin\"",
                    "sc.exe start JServicioTestV3"};
            ProcessBuilder pb = new ProcessBuilder();
            pb.redirectErrorStream(true);
            for (String i : cmd) {
                String serviceName = i.split(" ")[2]; //Se obtiene el nombre del servicio.
                pb.command(i.split(" "));
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    if (i.contains("sc.exe stop")) {
                        LOG.warn("No se logro bajar el servicio: " + serviceName + " es posible que ya se encontrara abajo.");
                    } else if (i.contains("sc.exe config")) {
                        LOG.error("No se logro refrescar las credenciales del servicio: " + serviceName);
                    } else if (i.contains("sc.exe start")) {
                        LOG.error("No se logro subir el servicio: " + serviceName);
                    }
                }
            }
            LOG.info("Se han reiniciado los servicios correctamente!!");
            System.out.println("Se han reiniciado los servicios correctamente!!");
        } catch (IOException | InterruptedException ex) {
            LOG.error("Se produjo un error reiniciando los servicios: " + ex);
        }
    }
}
