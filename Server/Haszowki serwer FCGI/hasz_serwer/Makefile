__start__: clean obj verify store_answers

obj:
	mkdir obj

LIBFCGI= /home/witold/cla/dyplom/malinowski/FCGI/
CPPFLAGS = -c -Wall -pedantic -std=c++11 -O3 -I${LIBFCGI}/include
LDFLAGS = -Wall -pedantic -std=c++11 -O3 -lnsl -lsocket
LIBRARIES = -lcurl

verify: obj/verify.o
	g++ ${LDFLAGS} obj/verify.o ${LIBFCGI}/lib/libfcgi++.a ${LIBFCGI}/lib/libfcgi.a ${LIBRARIES} -o verify.fcgi

obj/verify.o: src/verify.cpp
	g++ ${CPPFLAGS} src/verify.cpp -o obj/verify.o

store_answers: obj/store_answers.o
	g++ ${LDFLAGS} obj/store_answers.o ${LIBFCGI}/lib/libfcgi++.a ${LIBFCGI}/lib/libfcgi.a ${LIBRARIES} -o store_answers.fcgi

obj/store_answers.o: src/store_answers.cpp
	g++ ${CPPFLAGS} src/store_answers.cpp -o obj/store_answers.o

build_formatter:
	make -C ./tools/astyle_2.06/build/gcc/
	cp ./tools/astyle_2.06/build/gcc/bin/astyle tools/.

clean:
	rm -f obj/* core* *.fcgi
	find . -name \*~ -print -exec rm {} \;

clean_formatter:
	rm -f ./tools/astyle_2.06/build/gcc/obj/*
	rm -f ./tools/astyle_2.06/build/gcc/bin/*
	rm -f ./tools/astyle

cleanall: clean_formatter clean
	rm -f bak/*

reformat:
	./tools/astyle --options=./tools/astyle.conf --suffix=none ./*.cpp ./*.h

#tutaj jest zle z warunkiem kopiowania pliku
safe_reformat:
	./tools/astyle --options=./tools/astyle.conf --suffix=.bak ./*.cpp ./*.h
	if [ ! -d "./bak" ];then     \
		mkdir ./bak;         \
	fi
	if [ ! -f "./src/*.*.bak" ];then    \
		mv ./src/*.*.bak ./bak/.;   \
	fi


help:
	@echo "  Programy FCGI do aplikacji Haszowki. Wersja 1.0.0"
	@echo
	@echo "  Lista podcelow dla polecenia make:"
	@echo
	@echo "                   - (wywolanie bez specyfikacji celu) wymusza"
	@echo "                     kompilacje wszystkich programow FCGI."
	@echo "  verify           - buduje verify."
	@echo "  store_answers    - buduje store_answers."
	@echo "  build_formatter  - buduje program do formatowania kodu Artistic Style."
	@echo
	@echo "  clean            - usuwa produkty kompilacji oraz programy FCGI."
	@echo "  clean_formatter  - usuwa produkty kompilacji programu do formatowania kodu"
	@echo "                     oraz program do formatowania kodu."
	@echo "  cleanall         - usuwa produkty kompilacji, programy FCGI,"
	@echo "                     produkty kompilacji programu do formatowania kodu"
	@echo "                     oraz program do formatowania kodu."
	@echo
	@echo "  reformat         - przeformatowuje kod"
	@echo "  safe_reformat    - przeformatowuje kod tworzac kopie zapasowe"
	@echo "                     zmienionych plikow"
	@echo
	@echo "  help             - wyswietla niniejszy komunikat"
	@echo
 


