package utilidades;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import dominio.Bicicleta;
import dominio.Estacion;
import dominio.InformacionEstadistica;
import dominio.Recorrido;

public class GestorDeArchivos {
	
	
	private Enumeration<? extends ZipEntry> archivosCSV;
	private ZipEntry archivoCSV;
	private ZipFile archivoZip;
	private Boolean esPrimeraLecturaDelArchivoZip;
	private Boolean esPrimeraLecturaDelArchivoCSV;
	private BufferedReader bufferDeLectura;
	
	
	public ZipFile[] obtenerArchivosZip(String path) throws ZipException, IOException {
		File directorio = new File(path);
		comprobarPath(directorio.toPath());
		File[] listaArchivosEnDirectorio = directorio.listFiles();
		ZipFile[] archivosZip = new ZipFile[listaArchivosEnDirectorio.length];
		
		for(int i= 0; i< listaArchivosEnDirectorio.length ; i++){
			archivosZip[i] = new ZipFile(listaArchivosEnDirectorio[i]);
		}
		return archivosZip;
	}
	
	public ZipFile getArchivoZip() {
		return archivoZip;
	}

	public void asignarArchivoZipParaProcesar(ZipFile archivoZip) {
		esPrimeraLecturaDelArchivoZip = Boolean.TRUE;
		esPrimeraLecturaDelArchivoCSV = Boolean.TRUE;
		this.archivoZip = archivoZip;
	}

	public List<Bicicleta> obtenerListaDeBicicletas(Integer cantidad) throws IOException, ParseException{
		List<Bicicleta> bicicletas = new ArrayList<Bicicleta>();
		String registroLeido = null;
		Bicicleta bicicleta;
		
		if(esPrimeraLecturaDelArchivoZip){
			archivosCSV = leerArchivosCSVContenidosEnZip(archivoZip);
			esPrimeraLecturaDelArchivoZip = Boolean.FALSE;
		}
		if(esPrimeraLecturaDelArchivoCSV){
			archivoCSV = archivosCSV.nextElement();
			InputStream stream = archivoZip.getInputStream(archivoCSV);
		    InputStreamReader inputStreamReader = new InputStreamReader(stream);
		    bufferDeLectura = new BufferedReader(inputStreamReader);
		    bufferDeLectura.readLine();
		    esPrimeraLecturaDelArchivoCSV = Boolean.FALSE;
		}
		while(bicicletas.size() < cantidad && 
				(registroLeido = bufferDeLectura.readLine()) != null){
			bicicleta = generarBicicleta(registroLeido);
			bicicletas.add(bicicleta);
		}
	
		return bicicletas;
	}
	
	private Bicicleta generarBicicleta(String registroLeido) throws ParseException  {
		String[] campos = registroLeido.split(";");
		
		Integer bicicletaId = Integer.parseInt(campos[1]);
		Integer estacionOrigenId = Integer.parseInt(campos[3]);
		String estacionOrigenNombre = campos[4];
		Integer estacionDestinoId = Integer.parseInt(campos[6]);
		String estacionDestinoNombre = campos[7];
		Integer minutosRecorridos = Integer.parseInt(campos[8]);
		
		Estacion estacionOrigen = new Estacion(estacionOrigenId, estacionOrigenNombre);
		Estacion estacionDestino = new Estacion(estacionDestinoId,estacionDestinoNombre);
		Recorrido recorrido = new Recorrido(estacionOrigen, estacionDestino);
		recorrido.setMinutosRecorridos(minutosRecorridos);
		Bicicleta bicicleta = new Bicicleta(bicicletaId,recorrido);
		
		return bicicleta;
	}

	
	public void crearYMLCon(InformacionEstadistica info, String directorioDeTrabajo) throws IOException {
		
		File archivoYML = new File (directorioDeTrabajo + "/estadisticas.yml");
		FileWriter fw = new FileWriter (archivoYML);
		BufferedWriter bw = new BufferedWriter (fw);
		PrintWriter pw = new PrintWriter (bw);
		
		pw.println("Bicicletas mas usadas: ");
		this.escribirIdsBicicletasMaximas(info, pw);
		
		pw.println("Bicicletas menos usadas: ");
		this.escribirIdsBicicletasMinimas(info, pw);
		
		pw.println("Recorridos mas realizados: ");
		this.escribirIdRecorridosMasRealizados (info, pw);
		
		pw.println("Tiempo promedio de uso: " + info.getTiempoPromedio());
		pw.close();
	}

	private void escribirIdRecorridosMasRealizados(InformacionEstadistica info,
			PrintWriter pw) {
		
		Iterator <RecorridoDTO> iterador = info.recorridosMasRealizados().iterator();
		
		while (iterador.hasNext()){
			
			RecorridoDTO recorrido = iterador.next();
			pw.println("id origen:" + recorrido.getIdEstacionOrigen());
			pw.println("id destino" + recorrido.getIdEstacionDestino());
			pw.println("");
		}
	}

	private void escribirIdsBicicletasMinimas(InformacionEstadistica info,
			PrintWriter pw) {
		
		Iterator <Integer> iterador = info.bicicletasMenosUsadas().iterator();
		
		while (iterador.hasNext()){
			pw.println("id:" + iterador.next());
		}
		
		pw.println("");
	}

	private void escribirIdsBicicletasMaximas(InformacionEstadistica info,
			PrintWriter pw) {
		
		Iterator <Integer> iterador = info.bicicletasMasUsadas().iterator();
		
		while (iterador.hasNext()){
			pw.println("id:" + iterador.next());
		}
		
		pw.println("");
	}	
	
	private Enumeration<? extends ZipEntry> leerArchivosCSVContenidosEnZip(ZipFile zipFiles) {
		Enumeration<? extends ZipEntry> listaDeArchivosCSVEnZip =  zipFiles.entries();
		
		return listaDeArchivosCSVEnZip;
	}
	
	private void comprobarPath(Path path) {
		try{
			Boolean elDirectorioEsValido = ((Boolean) Files.getAttribute(path,
					"basic:isDirectory", LinkOption.NOFOLLOW_LINKS));
			if (!elDirectorioEsValido) {
				throw new IllegalArgumentException("El Path: "+ path + " no es un directorio");
			}
		} catch (IOException ioe) {
			throw new IllegalArgumentException("El directorio con el path: "+path+"no existe" );
		}
	}
	
}