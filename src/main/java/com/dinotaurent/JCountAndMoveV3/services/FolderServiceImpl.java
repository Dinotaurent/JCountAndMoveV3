package com.dinotaurent.JCountAndMoveV3.services;

import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;


/**
 * @author dandazme
 */
@Service
public class FolderServiceImpl implements IFolderService {

    //Variables y constantes
    private static final Logger LOG = Logger.getLogger(FolderServiceImpl.class);
    private static final String PATH_ENTRADA = "C:\\Users\\la-pu\\Desktop\\PruebasServicios\\Rutas";
    private static final String PATH_TEMP = "C:\\Users\\la-pu\\Desktop\\PruebasServicios\\Rutas\\temp";
    private int contadorEntrada;
    private int contadorTemp;
    private File pathEntrada = new File(PATH_ENTRADA);
    private File pathTemp = new File(PATH_TEMP);
    private boolean rezagado = false;
    private int cantidadAnterior = 0;
    private int cantidadAnterior2 = 0;
    private int contador;

    //Filtra los documentos de la ruta de entrada que no esten ocultos, terminen en dicha extension y que tengan peso.
    FileFilter filtro = (File file) -> !file.isHidden() && file.getName().endsWith(".txt") && file.length() > 0;

    @Scheduled(fixedRate = 5000)
    @Override
    public void contar() {

        //Se obtiene la cantidad de documentos en las rutas.
        contadorEntrada = pathEntrada.listFiles(filtro).length;
        contadorTemp = pathTemp.listFiles(filtro).length;

        LOG.info("Se encontraron: " + contadorEntrada + " Documentos en la ruta de entrada.");
        if (contadorEntrada > 20) {
            LOG.info("La cantidad de documentos supera el limite permitido en la ruta de entrada lo cual bloqueara los servicio, se procedera mover los documentos y reiniciar servicios.");
            obtenerNombresArchivos();
        } else {
            //Se valida que no existan rezagos en la ruta de entrada.
            try {
                if (contador < 3) {
                    if (contador == 0 || contadorEntrada == cantidadAnterior || contadorEntrada == cantidadAnterior2) {
                        contador++;
                    } else {
                        contador = 0;
                    }
                } else if (!rezagado) {
                    LOG.info("No se detecto movimiento durante mucho tiempo, se procedera a reiniciar los servicios.");
                    rezagado = true;
                    obtenerNombresArchivos();
                }
                cantidadAnterior2 = cantidadAnterior;
                cantidadAnterior = contadorEntrada;
            } catch (Exception ex) {
                LOG.error(ex);
            }
        }
    }

    @Override
    public void obtenerNombresArchivos() {
        LOG.info("Se ejecuta metodo obtenerNombresArchivos");
        System.out.println("Se obtienen los nombres");
        mover();
    }

    @Override
    public void mover() {
        LOG.info("Se ejecuta metodo mover");
        System.out.println("Se mueven los archivos");
        contador = 0;
        rezagado = false;
        reinciarServicios();
    }

    @Override
    public void reinciarServicios() {
        LOG.info("Se ejecuta metodo reiniciarServicios");
        System.out.println("Se reinician los servicios");
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

}
